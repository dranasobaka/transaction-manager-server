package io.transatron.transaction.manager.scheduler.partitioning.configuration;

import io.transatron.transaction.manager.scheduler.partitioning.locking.PostgresPartitionLock;
import io.transatron.transaction.manager.scheduler.partitioning.repository.JdbcPartitionLockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

@Configuration
public class PostgresPartitionLockConfiguration {

    @Bean
    public PostgresPartitionLock postgresPartitionLock(JdbcPartitionLockRepository jdbcPartitionLockRepository,
                                                       @Value("${transaction-manager.scheduler.partition.lock.ttlMillis:60000}") long lockTtlMillis) {
        return new PostgresPartitionLock(jdbcPartitionLockRepository, lockTtlMillis);
    }

    @Bean
    JdbcPartitionLockRepository jdbcPartitionLockRepository(NamedParameterJdbcOperations jdbcOperations) {
        return new JdbcPartitionLockRepository(jdbcOperations);
    }

}
