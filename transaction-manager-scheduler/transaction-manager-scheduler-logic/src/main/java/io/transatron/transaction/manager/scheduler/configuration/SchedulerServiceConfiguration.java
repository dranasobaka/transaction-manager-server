package io.transatron.transaction.manager.scheduler.configuration;

import io.transatron.transaction.manager.scheduler.notification.NotificationService;
import io.transatron.transaction.manager.scheduler.configuration.properties.SchedulerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerServiceConfiguration {

    @Bean
    @ConfigurationProperties("transaction-manager.scheduler")
    public SchedulerProperties schedulerServiceProperties() {
        return new SchedulerProperties();
    }

    @Bean
    public ThreadPoolTaskScheduler postgresNotificationTaskScheduler(NotificationService postgresNotificationService,
                                                                     SchedulerProperties schedulerServiceProperties) {
        return getThreadPoolTaskScheduler(postgresNotificationService, schedulerServiceProperties);
    }

    private ThreadPoolTaskScheduler getThreadPoolTaskScheduler(NotificationService notificationService,
                                                               SchedulerProperties schedulerServiceProperties) {
        var threadPoolTaskScheduler = new ThreadPoolTaskScheduler() {
            @EventListener(ApplicationReadyEvent.class)
            public void startNotificationService() {
                scheduleAtFixedRate(notificationService::notifySubscribers,
                                    schedulerServiceProperties.getCheckTriggersIntervalMillis());
            }
        };
        threadPoolTaskScheduler.setPoolSize(schedulerServiceProperties.getThreadPoolSize());
        threadPoolTaskScheduler.setThreadGroupName("scheduler-notifications");
        threadPoolTaskScheduler.setThreadNamePrefix("postgres-notifications");
        threadPoolTaskScheduler.initialize();
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(schedulerServiceProperties.isWaitForJobsToCompleteOnShutdown());
        threadPoolTaskScheduler.setAwaitTerminationSeconds(schedulerServiceProperties.getAwaitTerminationSeconds());

        return threadPoolTaskScheduler;
    }

}
