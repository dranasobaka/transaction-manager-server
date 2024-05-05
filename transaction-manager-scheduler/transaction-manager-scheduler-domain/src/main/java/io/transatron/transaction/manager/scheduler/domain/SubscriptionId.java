package io.transatron.transaction.manager.scheduler.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@AllArgsConstructor(staticName = "of")
public class SubscriptionId {

    @With
    @NotBlank
    @Size(max = 40)
    String id;

    @NotBlank
    @Size(max = 61)
    String eventType;

}
