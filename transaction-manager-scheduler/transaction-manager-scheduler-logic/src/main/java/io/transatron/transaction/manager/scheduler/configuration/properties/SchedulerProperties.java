package io.transatron.transaction.manager.scheduler.configuration.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.experimental.Delegate;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class SchedulerProperties {

    @Positive
    private long checkTriggersIntervalMillis = 1000;

    @Positive
    private int threadPoolSize = 1;

    @Positive
    private long payloadSize = 8_000;

    @Positive
    private int awaitTerminationSeconds = 5;

    private boolean waitForJobsToCompleteOnShutdown = true;

    @Valid
    @Delegate
    private SubscriptionValidation subscriptionValidation = new SubscriptionValidation();

}
