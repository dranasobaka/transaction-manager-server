package io.transatron.transaction.manager.scheduler.domain;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class PartitionMetadata {
    int partition;
    long count;
}

