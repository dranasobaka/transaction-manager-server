package io.transatron.transaction.manager.web3.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tron")
public record TronProperties(String grpc,
                             String grpcSolidity,
                             String privateKey,
                             String apiKey) {
}
