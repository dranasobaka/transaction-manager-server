package io.transatron.transaction.manager.schudeler.persistence.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.transatron.transaction.manager.schudeler.persistence.PostgresPersistenceFacade;
import io.transatron.transaction.manager.schudeler.persistence.converter.SubscriptionRowMapper;
import io.transatron.transaction.manager.schudeler.persistence.repository.PostgresPersistenceRepository;
import io.transatron.transaction.manager.scheduler.domain.EventTypeMetadata;
import io.transatron.transaction.manager.scheduler.domain.PartitionMetadata;
import io.transatron.transaction.manager.scheduler.domain.Subscription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

@Configuration
public class PostgresPersistenceConfiguration {

    @Bean
    public PostgresPersistenceFacade postgresPersistenceFacade(PostgresPersistenceRepository postgresPersistenceRepository) {
        return new PostgresPersistenceFacade(postgresPersistenceRepository);
    }

    @Bean
    public PostgresPersistenceRepository postgresPersistenceRepository(SubscriptionRowMapper subscriptionRowMapper,
                                                                       RowMapperResultSetExtractor<Subscription> subscriptionRowMapperResultSetExtractor,
                                                                       RowMapperResultSetExtractor<EventTypeMetadata> eventTypeMetadataRowMapperResultSetExtractor,
                                                                       RowMapperResultSetExtractor<PartitionMetadata> partitionMetadataRowMapperResultSetExtractor,
                                                                       NamedParameterJdbcOperations jdbc) {
        return new PostgresPersistenceRepository(subscriptionRowMapper,
                                                 subscriptionRowMapperResultSetExtractor,
                                                 eventTypeMetadataRowMapperResultSetExtractor,
                                                 partitionMetadataRowMapperResultSetExtractor,
                                                 jdbc);
    }

    @Bean
    public SubscriptionRowMapper subscriptionRowMapper(ObjectMapper mapper) {
        return new SubscriptionRowMapper(mapper);
    }

    @Bean
    public RowMapperResultSetExtractor<Subscription> subscriptionRowMapperResultSetExtractor(SubscriptionRowMapper subscriptionRowMapper) {
        return new RowMapperResultSetExtractor<>(subscriptionRowMapper);
    }

    @Bean
    public RowMapperResultSetExtractor<EventTypeMetadata> eventTypeMetadataRowMapperResultSetExtractor() {
        return new RowMapperResultSetExtractor<>(
            (resultSet, idx) -> EventTypeMetadata.builder()
                                                 .count(resultSet.getLong("count"))
                                                 .minTriggerTs(resultSet.getLong("min"))
                                                 .maxTriggerTs(resultSet.getLong("max"))
                                                 .eventType(resultSet.getString("event_type"))
                                                 .build()
        );
    }

    @Bean
    public RowMapperResultSetExtractor<PartitionMetadata> partitionMetadataRowMapperResultSetExtractor() {
        return new RowMapperResultSetExtractor<>(
            (resultSet, idx) -> PartitionMetadata.builder()
                                                 .count(resultSet.getLong("count"))
                                                 .partition(resultSet.getInt("s_partition"))
                                                 .build()
        );
    }

}
