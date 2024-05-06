package io.transatron.transaction.manager.domain;

public record PaymentInfo(String depositAddress,
                          Long availableEnergy,
                          Long availableBandwidth) {
}
