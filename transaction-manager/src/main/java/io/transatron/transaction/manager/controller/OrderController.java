package io.transatron.transaction.manager.controller;

import io.transatron.transaction.manager.controller.dto.CreateOrderRequest;
import io.transatron.transaction.manager.controller.dto.EstimateOrderRequest;
import io.transatron.transaction.manager.controller.dto.EstimateOrderResponse;
import io.transatron.transaction.manager.controller.dto.GetLastOrderResponse;
import io.transatron.transaction.manager.controller.dto.OrderDto;
import io.transatron.transaction.manager.logic.PaymentInfoService;
import io.transatron.transaction.manager.logic.OrderService;
import io.transatron.transaction.manager.mapper.OrderDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(path = "/public/v1/orders", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    private final OrderDtoMapper mapper;

    private final PaymentInfoService paymentInfoService;

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(CREATED)
    public void createOrder(@RequestBody CreateOrderRequest request) {
        service.createOrder(request.userTransactions(), request.paymentTransaction(), request.energy(), request.bandwidth(), request.fulfillFrom());
    }

    @PostMapping(path = "/estimate", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(OK)
    public EstimateOrderResponse estimateOrder(@RequestBody EstimateOrderRequest request) {
        var orderEstimation = service.estimateOrder(request.energy(), request.bandwidth(), request.fulfillFrom());

        return new EstimateOrderResponse(orderEstimation.priceUsdt());
    }

    @GetMapping
    @ResponseStatus(OK)
    public GetLastOrderResponse findLastOrderForWallet(@RequestParam("wallet_address") String walletAddress) {
        var paymentInfo = paymentInfoService.getPaymentInfo();

        var responseBuilder = GetLastOrderResponse.builder()
                .depositAddress(paymentInfo.depositAddress())
                .availableEnergy(paymentInfo.availableEnergy())
                .availableBandwidth(paymentInfo.availableBandwidth());

        service.findLastOrder(walletAddress)
               .ifPresent(order -> {
                   var dto = mapper.toDto(order);
                   responseBuilder.id(dto.id())
                                  .walletAddress(dto.walletAddress())
                                  .status(order.status())
                                  .fulfillFrom(dto.fulfillFrom())
                                  .transactions(dto.transactions());
               });

        return responseBuilder.build();
    }

    @GetMapping("/{order_id}")
    @ResponseStatus(OK)
    public OrderDto findOrderById(@PathVariable("order_id") UUID orderId) {
        var order = service.findOrderById(orderId);
        return mapper.toDto(order);
    }

}
