package io.transatron.transaction.manager.controller.dto;

import io.transatron.transaction.manager.domain.TransactionStatus;

public record TransactionDto(String id,
                             String to,
                             Long amount,
                             TransactionStatus status) {
}
