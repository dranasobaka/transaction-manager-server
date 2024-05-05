package io.transatron.transaction.manager.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.transatron.transaction.manager.domain.exception.ErrorsTable;
import io.transatron.transaction.manager.domain.exception.TransaTronException;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.Instant;

import static java.util.Objects.nonNull;

@Builder
public record ErrorResponse(String message,
                            String error,
                            String status,
                            Integer code,
                            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Timestamp timestamp) {

    public static ErrorResponse of(TransaTronException ex, HttpStatus httpStatus) {
        return of(ex.getMessage(), ex.getError(), httpStatus);
    }

    public static ErrorResponse of(String message, ErrorsTable error, HttpStatus httpStatus) {
        final var responseBuilder = ErrorResponse.builder()
                                                 .message(message)
                                                 .status(httpStatus.name())
                                                 .timestamp(Timestamp.from(Instant.now()));
        if (nonNull(error)) {
            responseBuilder.error(error.name())
                           .code(error.getErrorCode());
        }

        return responseBuilder.build();
    }

}
