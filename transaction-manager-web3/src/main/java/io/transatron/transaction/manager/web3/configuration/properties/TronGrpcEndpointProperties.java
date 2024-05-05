package io.transatron.transaction.manager.web3.configuration.properties;

public record TronGrpcEndpointProperties(String grpc,
                                         String grpcSolidity,
                                         String privateKey,
                                         String apiKey) {
}
