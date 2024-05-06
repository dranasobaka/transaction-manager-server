package io.transatron.transaction.manager.logic.model;

public record OrderUsdtEstimation(Long ownEnergy,
                                  Long externalEnergy,
                                  Long ownBandwidth,
                                  Long regularPriceUsdt,
                                  Long transatronPriceUsdt) {
}
