package io.transatron.transaction.manager.logic.model;

public record OrderEstimation(Long ownEnergy,
                              Long externalEnergy,
                              Long ownBandwidth,
                              Double regularPrice,
                              Double transatronPrice) {
}
