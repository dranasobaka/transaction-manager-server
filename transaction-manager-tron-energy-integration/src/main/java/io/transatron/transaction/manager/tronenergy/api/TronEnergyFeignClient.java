package io.transatron.transaction.manager.tronenergy.api;

import io.transatron.transaction.manager.tronenergy.api.dto.CreateOrderRequest;
import io.transatron.transaction.manager.tronenergy.api.dto.GetCreditResponse;
import io.transatron.transaction.manager.tronenergy.api.dto.GetInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "tron-energy", url = "${tron-energy.url}")
public interface TronEnergyFeignClient {

    @GetMapping(path = "/info")
    GetInfoResponse getInfo();

    @GetMapping(path = "/credit")
    GetCreditResponse getCredit(@RequestParam("address") String address);

    @PostMapping(path = "/order/new", consumes = APPLICATION_JSON_VALUE)
    String createNewOrder(@RequestBody CreateOrderRequest request);

}
