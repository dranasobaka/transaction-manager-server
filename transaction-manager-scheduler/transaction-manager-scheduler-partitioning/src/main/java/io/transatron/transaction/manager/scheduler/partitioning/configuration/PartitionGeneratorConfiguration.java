package io.transatron.transaction.manager.scheduler.partitioning.configuration;

import io.transatron.transaction.manager.scheduler.partitioning.PartitionGenerator;
import io.transatron.transaction.manager.scheduler.partitioning.RoundRobinPartitionGenerator;
import io.transatron.transaction.manager.scheduler.partitioning.configuration.properties.PartitionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PartitionGeneratorConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "scheduler.service")
    public PartitionProperties partitionProperties() {
        return new PartitionProperties();
    }

    @Bean
    public PartitionGenerator partitionGenerator(PartitionProperties partitionProperties) {
        return new RoundRobinPartitionGenerator(partitionProperties.getPartitionsCount());
    }
}
