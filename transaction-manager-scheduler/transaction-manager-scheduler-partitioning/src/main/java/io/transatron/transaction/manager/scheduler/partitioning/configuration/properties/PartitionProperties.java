package io.transatron.transaction.manager.scheduler.partitioning.configuration.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class PartitionProperties {

    @Positive
    @Max(999_999)
    private int partitionsCount = 8;

}
