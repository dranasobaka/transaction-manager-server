package io.transatron.transaction.manager.tronenergy.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class MarketDto {

    private Double availableEnergy;

    private Double availableFastEnergy;

    private List<MarketResourceByPrice> availableEnergyByPrice;

    private Long totalEnergy;

    private Long nextReleaseEnergy;

    private Double availableBandwidth;

    private Double availableFastBandwidth;

    private List<MarketResourceByPrice> availableBandwidthByPrice;

    private Long totalBandwidth;

    private Long nextReleaseBandwidth;

    private Double energyPerTrxFrozen;

    private Double bandwidthPerTrxFrozen;

    private Double trxPerEnergyFee;

    private Double trxPerBandwidthFee;

}
