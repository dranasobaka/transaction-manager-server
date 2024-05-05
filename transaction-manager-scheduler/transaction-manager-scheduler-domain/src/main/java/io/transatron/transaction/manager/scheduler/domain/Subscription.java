package io.transatron.transaction.manager.scheduler.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder(toBuilder = true)
public class Subscription {

    @With
    @NotNull
    @Valid
    SubscriptionId subscriptionId;

    @With
    long triggerTsMillis;

    SubscriptionType subscriptionType;

    Schedule schedule;

    Object payload;

    @With
    int version;

    @Size(max = 61)
    String partitionKey;

    @JsonIgnore
    public String getEventType() {
        return subscriptionId.getEventType();
    }
}
