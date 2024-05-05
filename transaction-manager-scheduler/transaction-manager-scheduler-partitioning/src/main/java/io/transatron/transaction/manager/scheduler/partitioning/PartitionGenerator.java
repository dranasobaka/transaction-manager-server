package io.transatron.transaction.manager.scheduler.partitioning;

import io.transatron.transaction.manager.scheduler.domain.Subscription;

import static org.springframework.util.ObjectUtils.isEmpty;

public interface PartitionGenerator {

    int nextPartition();

    int nextPartition(String partitionKey);

    default int nextPartition(Subscription subscription) {
        return isEmpty(subscription.getPartitionKey())
            ? nextPartition()
            : nextPartition(subscription.getPartitionKey());
    }

}
