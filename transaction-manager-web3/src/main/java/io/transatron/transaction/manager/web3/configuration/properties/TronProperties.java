package io.transatron.transaction.manager.web3.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tron")
public record TronProperties(TronHttpApiEndpointProperties httpApi,
                             TronHttpApiEndpointProperties grpc) {
}
