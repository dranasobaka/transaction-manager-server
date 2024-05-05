package io.transatron.transaction.manager.domain;

import java.util.List;

public record Order(String walletAddress,
                    OrderStatus status,
                    List<Transaction> transactions) {
}
