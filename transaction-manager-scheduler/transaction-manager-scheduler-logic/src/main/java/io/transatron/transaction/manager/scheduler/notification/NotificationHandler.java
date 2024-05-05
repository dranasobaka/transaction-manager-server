package io.transatron.transaction.manager.scheduler.notification;

import io.transatron.transaction.manager.scheduler.domain.Subscription;

import java.util.List;

public interface NotificationHandler {

    List<Subscription> handle(List<Subscription> subscriptions, long notifySubscribersTriggerMillis);
}
