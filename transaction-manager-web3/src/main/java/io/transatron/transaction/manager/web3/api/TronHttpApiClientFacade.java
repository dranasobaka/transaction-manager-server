package io.transatron.transaction.manager.web3.api;

import io.transatron.transaction.manager.web3.api.dto.BroadcastHexRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TronHttpApiClientFacade {

    private final TronHttpApiFeignClient tronHttpApiClient;

    public void broadcastHex(String transaction) {
        var request = new BroadcastHexRequest(transaction);
        tronHttpApiClient.broadcastHex(request);
    }

}
