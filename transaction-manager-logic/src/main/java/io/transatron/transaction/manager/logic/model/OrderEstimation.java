package io.transatron.transaction.manager.logic.model;

public record OrderEstimation(Long ownEnergy,
                              Long externalEnergy,
                              Long ownBandwidth,
                              Long regularPriceUsdt,
                              Long transatronPriceUsdt) {
}
