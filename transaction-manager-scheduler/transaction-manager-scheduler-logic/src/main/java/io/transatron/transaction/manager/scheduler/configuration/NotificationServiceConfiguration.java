package io.transatron.transaction.manager.scheduler.configuration;

import io.transatron.transaction.manager.scheduler.configuration.properties.SedaConfigurationProperties;
import io.transatron.transaction.manager.scheduler.notification.DefaultNotificationHandler;
import io.transatron.transaction.manager.scheduler.notification.NotificationHandler;
import io.transatron.transaction.manager.schudeler.persistence.PostgresPersistenceFacade;
import io.transatron.transaction.manager.scheduler.notification.DefaultNotificationService;
import io.transatron.transaction.manager.scheduler.notification.NotificationService;
import io.transatron.transaction.manager.scheduler.TriggeredSubscriptionsService;
import io.transatron.transaction.manager.scheduler.partitioning.configuration.properties.PartitionProperties;
import io.transatron.transaction.manager.scheduler.partitioning.locking.PostgresPartitionLock;
import org.apache.camel.ProducerTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class NotificationServiceConfiguration {

    @Bean
    public NotificationService postgresNotificationService(PostgresPartitionLock partitionLock,
                                                           PartitionProperties partitionProperties,
                                                           Clock clock,
                                                           TriggeredSubscriptionsService postgresTriggeredSubscriptionsService) {
        return new DefaultNotificationService(postgresTriggeredSubscriptionsService, partitionLock, partitionProperties, clock);
    }

    @Bean
    public NotificationHandler notificationHandler(ProducerTemplate producerTemplate,
                                                   SedaConfigurationProperties createTronEnergyOrderSedaProperties,
                                                   SedaConfigurationProperties fulfillOrderSedaProperties) {
        return new DefaultNotificationHandler(producerTemplate, fulfillOrderSedaProperties, createTronEnergyOrderSedaProperties);
    }

    @Bean
    public TriggeredSubscriptionsService postgresTriggeredSubscriptionsService(PostgresPersistenceFacade postgresPersistenceFacade,
                                                                               NotificationHandler notificationHandler) {
        return new TriggeredSubscriptionsService(postgresPersistenceFacade, notificationHandler);
    }

}
