package io.transatron.transaction.manager.scheduler.notification;

import io.transatron.transaction.manager.scheduler.configuration.CamelConfiguration;
import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.slf4j.MDC;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.transatron.transaction.manager.scheduler.domain.EventTypes.CREATE_TRON_ENERGY_ORDER;
import static io.transatron.transaction.manager.scheduler.domain.EventTypes.FULFILL_ORDER;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toSet;

@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationHandler implements NotificationHandler {

    private final ProducerTemplate producerTemplate;

    @Override
    public List<Subscription> handle(List<Subscription> triggeredSubscriptions, long notifySubscribersTriggerMillis) {
        var notifications = createNotifications(triggeredSubscriptions, notifySubscribersTriggerMillis);
        var notifiedSubscriptionIds = sendNotifications(notifications);

        return getNotifiedSubscriptions(triggeredSubscriptions, notifiedSubscriptionIds);
    }

    private Set<SubscriptionId> sendNotifications(List<Notification<Object>> notifications) {
        return notifications.stream()
                            .map(this::handleSendNotification)
                            .filter(Objects::nonNull)
                            .map(Notification::getSubscriptionId)
                            .collect(toSet());
    }

    private Notification<Object> handleSendNotification(Notification<Object> notification) {
        return switch (notification.getEventType()) {
            case FULFILL_ORDER -> {
                producerTemplate.sendBody(CamelConfiguration.SEDA_FULFILL_ORDER_ROUTE_ID, notification);
                yield notification;
            }
            case CREATE_TRON_ENERGY_ORDER -> {
                producerTemplate.sendBody(CamelConfiguration.SEDA_CREATE_TRON_ENERGY_ORDER_ROUTE_ID, notification);
                yield notification;
            }
            default -> null;
        };
    }

    private List<Subscription> getNotifiedSubscriptions(List<Subscription> subscriptions, Set<SubscriptionId> notifiedSubscriptionIds) {
        return subscriptions.stream()
                            .filter(subscription -> notifiedSubscriptionIds.contains(subscription.getSubscriptionId()))
                            .toList();
    }

    private List<Notification<Object>> createNotifications(List<Subscription> subscriptions, long notifySubscribersTriggerMillis) {
        return subscriptions.stream()
                            .sorted(comparingLong(Subscription::getTriggerTsMillis))
                            .peek(subscription -> logErrorNotification(subscription, notifySubscribersTriggerMillis))
                            .map(this::toNotification)
                            .toList();
    }

    private void logErrorNotification(Subscription subscription, long notifySubscribersTriggerMillis) {
        var triggerTsMillis = subscription.getTriggerTsMillis();
        if (triggerTsMillis + 2000 < notifySubscribersTriggerMillis) {
            try (var eventType = MDC.putCloseable("eventType", subscription.getEventType());
                 var eventId = MDC.putCloseable("eventId", subscription.getSubscriptionId().getId())) {
                log.warn("Subscription is delayed for {} seconds, {}", (notifySubscribersTriggerMillis - triggerTsMillis) / 1000, subscription);
            }
        }
    }

    private Notification<Object> toNotification(Subscription subscription) {
        return Notification.builder()
                           .subscriptionId(toSubscriptionId(subscription.getSubscriptionId()))
                           .triggerTsMillis(subscription.getTriggerTsMillis())
                           .payload(subscription.getPayload())
                           .partitionKey(subscription.getPartitionKey())
                           .build();
    }

    private SubscriptionId toSubscriptionId(SubscriptionId subscriptionId) {
        return SubscriptionId.builder()
                             .id(subscriptionId.getId())
                             .eventType(subscriptionId.getEventType())
                             .build();
    }

}
