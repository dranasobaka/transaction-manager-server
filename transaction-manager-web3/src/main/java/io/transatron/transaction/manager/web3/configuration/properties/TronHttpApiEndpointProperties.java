package io.transatron.transaction.manager.web3.configuration.properties;

public record TronHttpApiEndpointProperties(String baseUrl,
                                            String authHeaderName,
                                            String authHeaderValue,
                                            Integer requestDelayMillis) {
}
