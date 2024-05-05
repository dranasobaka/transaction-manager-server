package io.transatron.transaction.manager.schudeler.persistence;

import io.transatron.transaction.manager.schudeler.persistence.repository.PostgresPersistenceRepository;
import io.transatron.transaction.manager.scheduler.domain.Subscription;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class PostgresPersistenceFacade implements SubscriptionPersistenceFacade {

    private final PostgresPersistenceRepository postgresPersistenceRepository;

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        return postgresPersistenceRepository.findById(id);
    }

    @Override
    public void remove(SubscriptionId id) {
        postgresPersistenceRepository.remove(id);
    }

    @Override
    public void remove(SubscriptionId id, int version) {
        postgresPersistenceRepository.remove(id, version);
    }

    @Override
    public List<Subscription> findTriggeredSubscriptions(int partition, long notifySubscribersTriggerMillis) {
        return postgresPersistenceRepository.findTriggeredSubscriptions(partition, notifySubscribersTriggerMillis);
    }

    @Override
    public List<Subscription> findByIds(List<SubscriptionId> subscriptionIds) {
        return postgresPersistenceRepository.findAllById(subscriptionIds).stream()
                                            .filter(subscription -> subscriptionIds.contains(subscription.getSubscriptionId()))
                                            .toList();
    }

    @Override
    public void save(Subscription subscription, int nextPartition) {
        if (subscription.getSubscriptionId().getId().length() > 40) {
            log.warn("Subscription id is longer than 40 symbols. Fix it {}", subscription.getSubscriptionId());
        }

        postgresPersistenceRepository.save(subscription, nextPartition);
    }

}
