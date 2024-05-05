package io.transatron.transaction.manager.scheduler.domain;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class EventTypeMetadata {
    String eventType;
    long count;
    long minTriggerTs;
    long maxTriggerTs;
}
