package io.transatron.transaction.manager.scheduler.configuration;

import io.transatron.transaction.manager.schudeler.persistence.SubscriptionPersistenceFacade;
import io.transatron.transaction.manager.scheduler.DefaultSubscriptionService;
import io.transatron.transaction.manager.scheduler.SubscriptionService;
import io.transatron.transaction.manager.scheduler.partitioning.PartitionGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionServiceConfiguration {

    @Bean
    public SubscriptionService subscriptionService(SubscriptionPersistenceFacade subscriptionPersistenceFacadeStrategy,
                                                   PartitionGenerator partitionGenerator) {
        return new DefaultSubscriptionService(subscriptionPersistenceFacadeStrategy, partitionGenerator);
    }

}
