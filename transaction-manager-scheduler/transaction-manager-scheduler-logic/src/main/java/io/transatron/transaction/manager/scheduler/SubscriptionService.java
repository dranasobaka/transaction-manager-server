package io.transatron.transaction.manager.scheduler;

import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionType;

import java.util.List;
import java.util.Optional;

public interface SubscriptionService {

    Optional<Subscription> findSubscription(SubscriptionId subscriptionId, SubscriptionType subscriptionType);

    List<Subscription> findSubscriptions(List<SubscriptionId> subscriptionIds);

    void unsubscribe(SubscriptionId subscriptionId, SubscriptionType subscriptionType);

    void save(Subscription subscription);

}
