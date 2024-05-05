package io.transatron.transaction.manager.functional.steps;

import io.transatron.transation.manager.client.TronTransactionManagerClient;
import lombok.RequiredArgsConstructor;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ServiceSteps {

    private final TronTransactionManagerClient client;

    public void assertGetServiceData() {
        assertThat(client.get()).isEqualTo("service data");
    }
}