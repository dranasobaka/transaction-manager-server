package io.transatron.transaction.manager.web3.model;

import lombok.Builder;

@Builder
public record TronAccountResources(String address,
                                   String managerAddress,
                                   Long totalStakedEnergy,
                                   Long totalStakedBandwidth,
                                   Long availableEnergy,
                                   Long availableBandwidth) {
}
