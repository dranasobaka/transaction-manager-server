package io.transatron.transaction.manager.tronenergy.api.dto;

import lombok.Data;

@Data
public class GetInfoResponse {

    private String address;

    private MarketDto market;

    private PriceDto price;

}
