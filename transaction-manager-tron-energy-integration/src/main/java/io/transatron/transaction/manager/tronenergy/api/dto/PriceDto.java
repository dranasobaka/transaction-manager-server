package io.transatron.transaction.manager.tronenergy.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class PriceDto {

    private List<MarketOrderDto> openEnergy;

    private List<MarketOrderDto> fastEnergy;

    private List<MarketOrderDto> openBandwidth;

    private List<MarketOrderDto> fastBandwidth;

}
