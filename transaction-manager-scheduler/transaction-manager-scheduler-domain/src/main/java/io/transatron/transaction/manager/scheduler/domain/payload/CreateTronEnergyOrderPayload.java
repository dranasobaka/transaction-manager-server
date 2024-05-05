package io.transatron.transaction.manager.scheduler.domain.payload;

public record CreateTronEnergyOrderPayload(String walletAddress,
                                           Long energy) {
}
