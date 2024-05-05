package io.transatron.transaction.manager.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorsTable {

    // 400
    VALIDATION_FAILED(40010),
    CORRUPTED_PAYLOAD(40020),

    // 403
    PAYMENT_FAILED(40310),

    // 404
    RESOURCE_NOT_FOUND(40420),

    // 500
    INTERNAL_SERVER_ERROR(50010);

    private final int errorCode;

}
