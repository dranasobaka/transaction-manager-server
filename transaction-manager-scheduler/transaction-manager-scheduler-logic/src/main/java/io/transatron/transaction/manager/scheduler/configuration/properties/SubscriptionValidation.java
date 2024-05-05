package io.transatron.transaction.manager.scheduler.configuration.properties;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class SubscriptionValidation {

    @PositiveOrZero
    private int startTsOffsetInDays = 1;

    @PositiveOrZero
    private int endTsOffsetInDays = 365;
}