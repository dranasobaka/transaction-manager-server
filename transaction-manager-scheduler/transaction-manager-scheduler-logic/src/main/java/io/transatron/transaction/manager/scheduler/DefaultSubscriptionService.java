package io.transatron.transaction.manager.scheduler;

import io.transatron.transaction.manager.schudeler.persistence.SubscriptionPersistenceFacade;
import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionType;
import io.transatron.transaction.manager.scheduler.domain.exception.SubscriptionTypeMismatchException;
import io.transatron.transaction.manager.scheduler.partitioning.PartitionGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class DefaultSubscriptionService implements SubscriptionService {

    private final SubscriptionPersistenceFacade persistenceFacade;
    private final PartitionGenerator partitionGenerator;

    @Override
    public Optional<Subscription> findSubscription(SubscriptionId subscriptionId, SubscriptionType subscriptionType) {
        log.debug("About to find subscription {}", subscriptionId);
        return persistenceFacade.findById(subscriptionId)
                                .filter(storedSubscription -> checkSubscriptionType(storedSubscription, subscriptionType));
    }

    @Override
    public List<Subscription> findSubscriptions(List<SubscriptionId> subscriptionIds) {
        log.debug("About to find a batch of subscriptions {}", subscriptionIds);
        return persistenceFacade.findByIds(subscriptionIds);
    }

    @Override
    public void unsubscribe(SubscriptionId subscriptionId, SubscriptionType subscriptionType) {
        log.debug("About to un-subscribe {}", subscriptionId);
        persistenceFacade.findById(subscriptionId)
                .filter(storedSubscription -> checkSubscriptionType(storedSubscription, subscriptionType))
                .ifPresentOrElse(
                    storedSubscription -> {
                        persistenceFacade.remove(subscriptionId);
                        log.info("Un-subscribed {}", subscriptionId);
                    },
                    () -> log.debug("Subscription {} already removed", subscriptionId)
                );
    }

    @Override
    public void save(Subscription subscription) {
        log.debug("About to save subscription {}", subscription.getSubscriptionId());
        var nextTrigger = findNextTriggerTs(subscription);
        var subscriptionWithTriggerTs = subscription.withTriggerTsMillis(nextTrigger);
        var nextPartition = partitionGenerator.nextPartition(subscriptionWithTriggerTs);
        persistenceFacade.save(subscriptionWithTriggerTs, nextPartition);
        log.debug("Subscription saved {}", subscription.getSubscriptionId());
    }

    private long findNextTriggerTs(Subscription subscription) {
        var type = subscription.getSubscriptionType();
        return switch (type) {
            case ONE_TIME -> subscription.getTriggerTsMillis();
            default       -> throw new IllegalArgumentException("should never happen");
        };
    }

    private boolean checkSubscriptionType(Subscription storedSubscription, SubscriptionType expectedType) {
        if (storedSubscription.getSubscriptionType() != expectedType) {
            var errorMessage = "Subscription type mismatch: subscriptionId %s, actual type %s"
                               .formatted(storedSubscription.getSubscriptionId(), storedSubscription.getSubscriptionType());
            throw new SubscriptionTypeMismatchException(errorMessage);
        }
        return true;
    }
}
