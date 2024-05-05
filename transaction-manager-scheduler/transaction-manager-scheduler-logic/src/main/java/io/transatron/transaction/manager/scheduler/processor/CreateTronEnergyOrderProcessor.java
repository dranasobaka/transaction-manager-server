package io.transatron.transaction.manager.scheduler.processor;

import io.transatron.transaction.manager.scheduler.domain.payload.CreateTronEnergyOrderPayload;
import io.transatron.transaction.manager.scheduler.notification.Notification;
import io.transatron.transaction.manager.tronenergy.TronEnergyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

@Slf4j
@RequiredArgsConstructor
public class CreateTronEnergyOrderProcessor implements Processor {

    private final TronEnergyManager tronEnergyManager;

    @Override
    public void process(Exchange exchange) throws Exception {
        var notification = exchange.getIn(Notification.class);
        var payload = (CreateTronEnergyOrderPayload) notification.getPayload();

        log.info("Creating TronEnergy order [wallet={}, energy amount={}]", payload.walletAddress(), payload.energy());
        tronEnergyManager.newEnergyOrder(payload.walletAddress(), payload.energy());
    }
}
