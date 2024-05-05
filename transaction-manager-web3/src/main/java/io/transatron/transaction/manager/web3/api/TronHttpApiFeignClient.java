package io.transatron.transaction.manager.web3.api;

import io.transatron.transaction.manager.web3.api.dto.BroadcastHexRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "tron-http-api", url = "${tron.http-api.url}")
public interface TronHttpApiFeignClient {

    @PostMapping(
        path = "/wallet/broadcasthex",
        headers = {
            "Content-Type: application/json"
        }
    )
    void broadcastHex(@RequestBody BroadcastHexRequest request);

}
