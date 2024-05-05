package io.transatron.transaction.manager.scheduler.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.transatron.transaction.manager.scheduler.domain.SubscriptionId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder(toBuilder = true)
public class Notification<T> {

    @NotNull
    SubscriptionId subscriptionId;

    @With
    @Positive
    long triggerTsMillis;

    @NotNull
    T payload;

    @JsonIgnore
    String partitionKey;

    public String getEventType() {
        return subscriptionId.getEventType();
    }

}
