package io.transatron.transaction.manager.logic.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "transaction-manager.constraints")
public class ConstraintsProperties {

    private Duration orderMinDelay = Duration.ofMinutes(5);

    private Duration orderMaxDelay = Duration.ofHours(47);

}
