package io.transatron.transaction.manager.schudeler.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.transatron.transaction.manager.scheduler.domain.Schedule;
import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

@RequiredArgsConstructor
public class SubscriptionRowMapper implements RowMapper<Subscription> {

    private final ObjectMapper mapper;

    @Override
    public Subscription mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            var subscriptionType = getSubscriptionType(resultSet);
            return Subscription.builder()
                               .subscriptionId(getSubscriptionId(resultSet))
                               .subscriptionType(subscriptionType)
                               .triggerTsMillis(resultSet.getLong("trigger_ts_millis"))
                               .payload(deserializePayload(resultSet.getString("payload")))
                               .schedule(getSchedule(resultSet, subscriptionType))
                               .version(resultSet.getInt("versioning"))
                               .partitionKey(resultSet.getString("partition_key"))
                               .build();
    }

    public String serializePayload(Object payload) {
        if (isNull(payload)) {
            return null;
        }
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private SubscriptionType getSubscriptionType(ResultSet resultSet) throws SQLException {
        var subscriptionType = resultSet.getString("subscription_type");
        if (isEmpty(subscriptionType)) {
            throw new IllegalArgumentException("Subscription type can not be null or empty");
        }
        return SubscriptionType.findByValue(subscriptionType);
    }

    private SubscriptionId getSubscriptionId(ResultSet resultSet) throws SQLException {
        return SubscriptionId.builder()
                             .id(resultSet.getString("event_id"))
                             .eventType(resultSet.getString("event_type"))
                             .build();
    }

    private Schedule getSchedule(ResultSet resultSet, SubscriptionType subscriptionType) throws SQLException {
        return subscriptionType == SubscriptionType.ONE_TIME
            ? null
            : Schedule.builder()
                      .fixedRateMillis(resultSet.getLong("schedule_fixed_rate_millis"))
                      .cron(resultSet.getString("schedule_cron"))
                      .startTsMillis(resultSet.getLong("start_ts_millis"))
                      .endTsMillis(resultSet.getLong("end_ts_millis"))
                      .build();
    }

    private Object deserializePayload(String payloadJson) {
        if (isNull(payloadJson)) {
            return null;
        }
        try {
            return mapper.readValue(payloadJson, Object.class);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
