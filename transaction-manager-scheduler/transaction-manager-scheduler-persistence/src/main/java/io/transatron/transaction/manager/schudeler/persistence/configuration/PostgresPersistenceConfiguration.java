package io.transatron.transaction.manager.schudeler.persistence.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.transatron.transaction.manager.schudeler.persistence.PostgresPersistenceFacade;
import io.transatron.transaction.manager.schudeler.persistence.converter.SubscriptionRowMapper;
import io.transatron.transaction.manager.schudeler.persistence.repository.PostgresPersistenceRepository;
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
                                                                       NamedParameterJdbcOperations jdbc) {
        return new PostgresPersistenceRepository(subscriptionRowMapper,
                                                 subscriptionRowMapperResultSetExtractor,
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

}
