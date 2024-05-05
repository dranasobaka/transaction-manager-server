package io.transatron.transaction.manager.scheduler.notification;

import io.transatron.transaction.manager.scheduler.TriggeredSubscriptionsService;
import io.transatron.transaction.manager.scheduler.partitioning.PartitionLock;
import io.transatron.transaction.manager.scheduler.partitioning.configuration.properties.PartitionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final TriggeredSubscriptionsService processTriggeredSubscriptions;
    private final PartitionLock partitionLock;
    private final PartitionProperties properties;
    private final Clock clock;

    @Override
    public void notifySubscribers() {
        var notifySubscribersInstant = clock.instant();
        var notifySubscribersTriggerMillis = notifySubscribersInstant.toEpochMilli();
        var startFromPartition = ThreadLocalRandom.current().nextInt(properties.getPartitionsCount());
        IntStream.range(startFromPartition, properties.getPartitionsCount() + startFromPartition)
                 .map(this::getPartitionLockId)
                 .forEachOrdered(partitionLockId -> tryProcess(partitionLockId, notifySubscribersTriggerMillis, notifySubscribersInstant));
    }

    private void tryProcess(int partitionLockId, long notifySubscribersTriggerMillis, Instant notifySubscribersInstant) {
        if (partitionLock.tryLock(partitionLockId, notifySubscribersInstant)) {
            try {
                processTriggeredSubscriptions.processTriggeredSubscriptions(partitionLockId, notifySubscribersTriggerMillis);
            } finally {
                partitionLock.unlock(partitionLockId);
            }
        }
    }

    private int getPartitionLockId(int partitionRaw) {
        return partitionRaw % properties.getPartitionsCount();
    }

}
