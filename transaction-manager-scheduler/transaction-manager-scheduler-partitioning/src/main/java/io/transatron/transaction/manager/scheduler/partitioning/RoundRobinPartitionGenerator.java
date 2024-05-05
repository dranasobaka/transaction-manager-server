package io.transatron.transaction.manager.scheduler.partitioning;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class RoundRobinPartitionGenerator implements PartitionGenerator {

    private final AtomicInteger partitionGenerator;
    private final int partitionCount;

    public RoundRobinPartitionGenerator(int partitionCount) {
        this.partitionCount = partitionCount;
        this.partitionGenerator = new AtomicInteger(-1);
    }

    @Override
    public int nextPartition() {
        int current, next;
        do {
            current = partitionGenerator.get();
            next = current < Integer.MAX_VALUE ? current + 1 : 0;
        } while (!partitionGenerator.compareAndSet(current, next));
        return next % partitionCount;
    }

    @Override
    public int nextPartition(String partitionKey) {
        return Math.abs(partitionKey.hashCode()) % partitionCount;
    }
}
