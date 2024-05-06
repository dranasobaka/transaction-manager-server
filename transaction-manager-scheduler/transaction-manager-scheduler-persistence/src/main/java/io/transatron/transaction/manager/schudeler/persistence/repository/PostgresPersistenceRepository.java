package io.transatron.transaction.manager.schudeler.persistence.repository;

import io.transatron.transaction.manager.schudeler.persistence.converter.SubscriptionRowMapper;
import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;
import io.transatron.transaction.manager.scheduler.domain.exception.SubscriptionTypeMismatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@RequiredArgsConstructor
public class PostgresPersistenceRepository {

    private static final String UPSERT_SUBSCRIPTION_QUERY_TEMPLATE =
        """
        INSERT INTO subscriptions(event_id, service_name, event_type, subscription_type, s_partition, trigger_ts_millis,
                                  payload, schedule_cron, schedule_fixed_rate_millis, start_ts_millis, end_ts_millis, versioning,
                                  partition_key)
        VALUES (:event_id, :service_name, :event_type, :subscription_type, :s_partition, :trigger_ts_millis, :payload,
                :schedule_cron, :schedule_fixed_rate_millis, :start_ts_millis, :end_ts_millis, 0, :partition_key)
        ON CONFLICT (event_id, event_type) DO UPDATE
        SET s_partition       = CASE WHEN subscriptions.subscription_type = :subscription_type THEN EXCLUDED.s_partition ELSE subscriptions.s_partition END,
            trigger_ts_millis = CASE WHEN subscriptions.subscription_type = :subscription_type THEN EXCLUDED.trigger_ts_millis ELSE subscriptions.trigger_ts_millis END,
            payload           = CASE WHEN subscriptions.subscription_type = :subscription_type THEN EXCLUDED.payload ELSE subscriptions.payload END,
            schedule_cron     = CASE WHEN subscriptions.subscription_type = :subscription_type THEN EXCLUDED.schedule_cron ELSE subscriptions.schedule_cron END,
            schedule_fixed_rate_millis = CASE WHEN subscriptions.subscription_type = :subscription_type THEN EXCLUDED.schedule_fixed_rate_millis ELSE subscriptions.schedule_fixed_rate_millis END,
            start_ts_millis   = CASE WHEN subscriptions.subscription_type = :subscription_type THEN EXCLUDED.start_ts_millis ELSE subscriptions.start_ts_millis END,
            end_ts_millis     = CASE WHEN subscriptions.subscription_type = :subscription_type THEN EXCLUDED.end_ts_millis ELSE subscriptions.end_ts_millis END,
            versioning        = CASE WHEN subscriptions.subscription_type = :subscription_type THEN subscriptions.versioning + 1 ELSE subscriptions.versioning END,
            handled           = false,
            partition_key     = CASE WHEN subscriptions.subscription_type = :subscription_type THEN EXCLUDED.partition_key ELSE subscriptions.partition_key END
        """;

    private final SubscriptionRowMapper subscriptionRowMapper;
    private final RowMapperResultSetExtractor<Subscription> subscriptionRowMapperResultSetExtractor;
    private final NamedParameterJdbcOperations jdbc;

    public Optional<Subscription> findById(SubscriptionId id) {
        try {
            var subscription = jdbc.queryForObject("""
                                                   SELECT *
                                                   FROM subscriptions
                                                   WHERE event_id = :event_id 
                                                     AND event_type = :event_type 
                                                     AND handled = false
                                                   """,
                                                   getIdParamMap(id),
                                                   subscriptionRowMapper);
            return Optional.ofNullable(subscription);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public int remove(SubscriptionId id) {
        return jdbc.update("""
                           UPDATE subscriptions
                           SET handled = true
                           WHERE event_id = :event_id
                             AND event_type = :event_type
                           """,
                           getIdParamMap(id));
    }

    public int remove(SubscriptionId id, int version) {
        return jdbc.update("""
                           update subscriptions
                           set handled = true
                           where event_id = :event_id
                           and event_type = :event_type
                           and versioning = :versioning
                           """,
                           getIdParamMapWithVersion(id, version));
    }

    public int removeHandled(long millis) {
        var params = Map.of("millis", millis);
        var removed = jdbc.update("""
                                  DELETE 
                                  FROM subscriptions
                                  WHERE trigger_ts_millis <= :millis
                                    AND handled = true
                                  """,
                                  params);
        log.debug("Removed {} handled subscriptions", removed);
        return removed;
    }

    public List<Subscription> findTriggeredSubscriptions(int partition, long millis) {
        return jdbc.query("""
                          SELECT * 
                          FROM subscriptions 
                          WHERE s_partition = :s_partition 
                            AND handled = false
                            AND trigger_ts_millis <= :trigger_ts_millis_before 
                          ORDER BY trigger_ts_millis
                          LIMIT 10000
                          """,
                          getPartitionParams(partition, millis),
                          subscriptionRowMapperResultSetExtractor);
    }

    public List<Subscription> findAllById(List<SubscriptionId> subscriptionIds) {
        return jdbc.query("""
                          SELECT * 
                          FROM subscriptions 
                          WHERE event_id IN (:ids) 
                            AND handled = false 
                          LIMIT 1000
                          """,
                          getIdsParamMap(subscriptionIds),
                          subscriptionRowMapperResultSetExtractor);
    }

    public void save(Subscription subscription, int nextPartition) {
        var subscriptionId = subscription.getSubscriptionId();
        var before = findSubscriptionByIdAndType(subscriptionId.getId(), subscriptionId.getEventType());
        var updated = jdbc.update(UPSERT_SUBSCRIPTION_QUERY_TEMPLATE, getParamsMap(subscription, nextPartition));
        var after = findSubscriptionByIdAndType(subscriptionId.getId(), subscriptionId.getEventType());

        // Unfortunately we cannot rely on return from "update(...)" method in a case of PostgreSQL,
        // because even if no actual update happened in UPSERT - we still receive an affected row.
        // Therefore, we comparing subscriptions before and after an UPSERT has been executed.
        if (updated == 0 || (nonNull(before) && before.equals(after))) {
            throw new SubscriptionTypeMismatchException("Wrong type for " + subscription.getSubscriptionId());
        }
    }

    private Subscription findSubscriptionByIdAndType(String eventId, String eventType) {
        var params = Map.of("id", eventId,
                            "type", eventType);
        try {
            return jdbc.queryForObject("SELECT * FROM subscriptions WHERE event_id = :id AND event_type = :type",
                                       params,
                                       subscriptionRowMapper);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Map<String, ? extends Serializable> getParamsMap(Subscription subscription, int nextPartition) {
        var subscriptionId = subscription.getSubscriptionId();
        var schedule = subscription.getSchedule();
        var params = new HashMap<String, Serializable>();
        params.put("event_id", subscriptionId.getId());
        params.put("service_name", "hackathon_demo_service");
        params.put("event_type", subscriptionId.getEventType());
        params.put("subscription_type", subscription.getSubscriptionType().getValue());
        params.put("partition_key", subscription.getPartitionKey());
        params.put("s_partition", nextPartition);
        params.put("trigger_ts_millis", subscription.getTriggerTsMillis());
        params.put("payload", subscriptionRowMapper.serializePayload(subscription.getPayload()));
        params.put("schedule_cron", isNull(schedule) ? null : schedule.getCron());
        params.put("schedule_fixed_rate_millis", isNull(schedule) ? null : schedule.getFixedRateMillis());
        params.put("start_ts_millis", isNull(schedule) ? null : schedule.getStartTsMillis());
        params.put("end_ts_millis", isNull(schedule) ? null : schedule.getEndTsMillis());
        return params;
    }

    private Map<String, String> getIdParamMap(SubscriptionId id) {
        return Map.of("event_id", id.getId(),
                      "event_type", id.getEventType());
    }

    private Map<String, Object> getIdParamMapWithVersion(SubscriptionId id, int version) {
        return Map.of("event_id", id.getId(),
                      "event_type", id.getEventType(),
                      "versioning", version);
    }

    private Map<String, List<String>> getIdsParamMap(List<SubscriptionId> ids) {
        var subscriptionIds = ids.stream()
                                 .map(SubscriptionId::getId)
                                 .toList();
        return Map.of("ids", subscriptionIds);
    }

    private Map<String, ? extends Number> getPartitionParams(int partition, long millis) {
        return Map.of("s_partition", partition,
                      "trigger_ts_millis_before", millis);
    }

}
