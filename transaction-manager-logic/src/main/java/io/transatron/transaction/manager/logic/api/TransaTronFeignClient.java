package io.transatron.transaction.manager.logic.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "transatron", url = "${transatron.url}")
public interface TransaTronFeignClient {

    @PostMapping(path = "/wallet/broadcasthex", consumes = APPLICATION_JSON_VALUE)
    String broadcastHexTransaction(@RequestBody String rawTransaction);

}
