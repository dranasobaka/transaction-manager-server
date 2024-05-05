package io.transatron.transaction.manager.web3.configuration;

import io.transatron.transaction.manager.web3.configuration.properties.TronGrpcEndpointProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tron.trident.core.ApiWrapper;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Slf4j
@Configuration
public class TronClientConfiguration {

    @Bean
    public ApiWrapper apiWrapper(TronGrpcEndpointProperties properties) {
        return createApiWrapper(properties);
    }

    private ApiWrapper createApiWrapper(TronGrpcEndpointProperties properties) {
        var grpcEndpoint = properties.grpc();
        var grpcEndpointSolidity = properties.grpcSolidity();
        var privateKey = properties.privateKey();
        var apiKey = properties.apiKey();

        log.info("Using direct gRPC [{}] and gRPC Solidity [{}]", grpcEndpoint, grpcEndpointSolidity);
        return isNotEmpty(apiKey)
            ? new ApiWrapper(grpcEndpoint, grpcEndpointSolidity, privateKey, apiKey)
            : new ApiWrapper(grpcEndpoint, grpcEndpointSolidity, privateKey);
    }

}
