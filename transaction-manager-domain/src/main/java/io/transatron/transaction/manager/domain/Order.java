package io.transatron.transaction.manager.domain;

import java.util.List;
import java.util.UUID;

public record Order(UUID id,
                    String walletAddress,
                    OrderStatus status,
                    List<Transaction> transactions) {
}
