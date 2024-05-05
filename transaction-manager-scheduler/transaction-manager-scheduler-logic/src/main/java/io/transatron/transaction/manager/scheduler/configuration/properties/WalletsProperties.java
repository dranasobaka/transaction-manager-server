package io.transatron.transaction.manager.scheduler.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * I know this is stupid and insecure to supply private keys via properties file,
 * so this approach is only valid for PoC on hackathon
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wallets")
public class WalletsProperties {

    private Map<String, String> privateKeys;

}
