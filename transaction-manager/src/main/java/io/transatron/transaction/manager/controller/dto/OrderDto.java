package io.transatron.transaction.manager.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.transatron.transaction.manager.domain.OrderStatus;

import java.sql.Timestamp;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderDto(String walletAddress,
                       OrderStatus status,
                       @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Timestamp fulfillFrom,
                       List<TransactionDto> transactions) {
}
