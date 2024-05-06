package io.transatron.transaction.manager.controller.dto;

public record EstimateOrderResponse(Long regularPriceUsdt,
                                    Long transatronPriceUsdt) {
}
