package io.transatron.transaction.manager.scheduler;

import io.transatron.transaction.manager.schudeler.persistence.repository.PostgresPersistenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class HandledNotificationsRemover {

    private final ThreadPoolTaskScheduler handledSubscriptionDeletionExecutor;
    private final PostgresPersistenceRepository postgresPersistenceRepository;
    private final long removeHandledInterval;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    public void beginRemoval() {
        log.info("Starting clear handled subscriptions job");
        startJob();
    }

    private void startJob() {
        handledSubscriptionDeletionExecutor.scheduleAtFixedRate(this::removeHandled, Duration.ofMillis(removeHandledInterval));
    }

    private void removeHandled() {
        postgresPersistenceRepository.removeHandled(clock.millis());
    }

}
