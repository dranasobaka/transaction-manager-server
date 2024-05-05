package io.transatron.transaction.manager.functional;

import io.transatron.transation.manager.client.TronTransactionManagerClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class DummyLogicTest extends BaseFunctionalTest {

    @Autowired
    private TronTransactionManagerClient client;

    @Value("${transaction-manager.spring.kafka.producer.topic-name}")
    private String producerTopic;

    @Test
    void shouldGetServiceData() {
        serviceSteps.assertGetServiceData();
    }

    @Test
    void shouldSaveServiceData() {
        client.save();

        // TODO: Kafka consumer assertions
    }

}