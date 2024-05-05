package io.transatron.transaction.manager.scheduler.partitioning;

import java.time.Instant;

public interface PartitionLock {

    boolean tryLock(int partition, Instant notifySubscribersInstant);

    boolean unlock(int partition);
}
