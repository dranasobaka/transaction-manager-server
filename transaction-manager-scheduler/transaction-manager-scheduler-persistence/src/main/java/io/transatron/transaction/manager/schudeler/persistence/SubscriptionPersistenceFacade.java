package io.transatron.transaction.manager.schudeler.persistence;

import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPersistenceFacade {

    void save(Subscription subscription, int nextPartition);

    void remove(SubscriptionId subscriptionId);

    void remove(SubscriptionId subscriptionId, int version);

    Optional<Subscription> findById(SubscriptionId subscriptionId);

    List<Subscription> findTriggeredSubscriptions(int partition, long notifySubscribersTriggerMillis);

    List<Subscription> findByIds(List<SubscriptionId> subscriptionIds);
}
