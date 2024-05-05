package io.transatron.transaction.manager.tronenergy.api.dto;

import lombok.Data;

@Data
public class MarketOrderDto {

    private Long minDuration;

    private Long basePrice;

    private Long minPoolPrice;

    private Long suggestedPrice;

}
