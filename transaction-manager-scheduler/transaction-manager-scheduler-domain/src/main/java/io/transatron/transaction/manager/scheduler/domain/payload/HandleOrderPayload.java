package io.transatron.transaction.manager.scheduler.domain.payload;

import java.util.UUID;

public record HandleOrderPayload(UUID orderId) {
}
