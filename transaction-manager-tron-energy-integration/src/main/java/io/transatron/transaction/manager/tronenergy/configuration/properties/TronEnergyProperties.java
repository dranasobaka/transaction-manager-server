package io.transatron.transaction.manager.tronenergy.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tron-energy")
public class TronEnergyProperties {

    private String url;

    private String marketWalletAddress;

    private String apiKey;

}
