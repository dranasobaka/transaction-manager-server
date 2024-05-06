package io.transatron.transaction.manager.logic.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment-info")
public class PaymentInfoProperties {

    private String depositAddress;

}
