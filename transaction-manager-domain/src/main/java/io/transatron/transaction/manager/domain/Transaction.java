package io.transatron.transaction.manager.domain;

import lombok.Builder;

@Builder
public record Transaction(String id,
                          String from,
                          String to,
                          Long amount,
                          TransactionStatus status,
                          Long estimatedEnergy,
                          Long estimatedBandwidth,
                          String rawTransaction) {
}
