package io.transatron.transaction.manager.logic.model;

public record ResourcesEstimation(Long ownEnergy,
                                  Long externalEnergy,
                                  Long ownBandwidth) {
}
