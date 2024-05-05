package io.transatron.transaction.manager.scheduler;

import io.transatron.transaction.manager.schudeler.persistence.SubscriptionPersistenceFacade;
import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionType;
import io.transatron.transaction.manager.scheduler.notification.NotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@RequiredArgsConstructor
public class TriggeredSubscriptionsService {

    private final SubscriptionPersistenceFacade persistenceFacade;
    private final NotificationHandler notificationHandler;

    public void processTriggeredSubscriptions(int partition, long notifySubscribersTriggerMillis) {
        List<Subscription> triggeredSubscriptions;
        do {
            triggeredSubscriptions = persistenceFacade.findTriggeredSubscriptions(partition, notifySubscribersTriggerMillis);
            if (triggeredSubscriptions.isEmpty()) {
                break;
            }

            log.info("Notifying {} triggered subscriptions. There are: {}", triggeredSubscriptions.size(), getTriggeredSubscriptionsCount(triggeredSubscriptions));
            var notifiedSubscriptions = notificationHandler.handle(triggeredSubscriptions, notifySubscribersTriggerMillis);

            updateOrRemove(notifiedSubscriptions);
        } while (!triggeredSubscriptions.isEmpty());
    }

    // TODO: create route in Apache Camel for deleting subscriptions (should be triggered when operation is done) ?

    private static Map<String, Long> getTriggeredSubscriptionsCount(List<Subscription> triggeredSubscriptions) {
        return triggeredSubscriptions.stream()
                    .map(Subscription::getEventType)
                    .collect(groupingBy(identity(), counting()));
    }

    private void updateOrRemove(List<Subscription> notifiedSubscriptions) {
        for (var subscription : notifiedSubscriptions) {
            try (var ignored = MDC.putCloseable("eventType", subscription.getEventType());
                 var ignored1 = MDC.putCloseable("eventId", subscription.getSubscriptionId().getId())) {
                updateOrRemove(subscription);
            }
        }
    }

    private void updateOrRemove(Subscription subscription) {
        var type = subscription.getSubscriptionType();
        if (Objects.requireNonNull(type) == SubscriptionType.ONE_TIME) {
            removeOneTimeSubscription(subscription);
        }
    }

    private void removeOneTimeSubscription(Subscription subscription) {
        log.info("Removing one-time subscription '{}'", subscription.getSubscriptionId());
        persistenceFacade.remove(subscription.getSubscriptionId(), subscription.getVersion());
    }

}
