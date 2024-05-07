package io.transatron.transaction.manager.domain;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public record Order(UUID id,
                    String walletAddress,
                    OrderStatus status,
                    List<Transaction> transactions,
                    Long costUsdt,
                    Long energy,
                    Long bandwidth,
                    Timestamp fulfillFrom) {
}
