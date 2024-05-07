package io.transatron.transaction.manager.scheduler.processor;

import io.transatron.transaction.manager.scheduler.domain.payload.CreateTronEnergyOrderPayload;
import io.transatron.transaction.manager.scheduler.notification.Notification;
import io.transatron.transaction.manager.tronenergy.TronEnergyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.LinkedHashMap;

@Slf4j
@RequiredArgsConstructor
public class CreateTronEnergyOrderProcessor implements Processor {

    private final TronEnergyManager tronEnergyManager;

    @Override
    public void process(Exchange exchange) throws Exception {
        var notification = exchange.getIn().getBody(Notification.class);
        var payload = (LinkedHashMap) notification.getPayload();
        var walletAddress = (String) payload.get("walletAddress");
        var energy = (Long) payload.get("energy");

        log.info("Creating TronEnergy order [wallet={}, energy amount={}]", walletAddress, energy);
        tronEnergyManager.newEnergyOrder(walletAddress, energy);
        log.info("Created TronEnergy order [wallet={}, energy amount={}]", walletAddress, energy);
    }
}
