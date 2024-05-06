package io.transatron.transaction.manager.scheduler.configuration;

import io.transatron.transaction.manager.schudeler.persistence.repository.PostgresPersistenceRepository;
import io.transatron.transaction.manager.scheduler.HandledNotificationsRemover;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;

@Configuration
public class SchedulerServicePostgresConfiguration {

    @Value("${transaction-manager.scheduler.persistence.postgres.remove-handled.intervalMillis:10000}")
    private long removeHandledInterval;

    @Bean
    public ThreadPoolTaskScheduler handledSubscriptionDeletionExecutor() {
        var scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadGroupName("scheduler-notifications");
        scheduler.setThreadNamePrefix("handled-remover");
        scheduler.initialize();
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);

        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean(HandledNotificationsRemover.class)
    public HandledNotificationsRemover handledNotificationsRemover(ThreadPoolTaskScheduler handledSubscriptionDeletionExecutor,
                                                                   PostgresPersistenceRepository repository,
                                                                   Clock clock) {
        return new HandledNotificationsRemover(handledSubscriptionDeletionExecutor, repository, removeHandledInterval, clock);
    }

}
