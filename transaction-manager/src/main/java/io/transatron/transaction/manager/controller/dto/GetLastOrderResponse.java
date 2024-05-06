package io.transatron.transaction.manager.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.transatron.transaction.manager.domain.OrderStatus;
import lombok.Builder;

import java.sql.Timestamp;
import java.util.List;

@Builder
public record GetLastOrderResponse(String id,
                                   String walletAddress,
                                   OrderStatus status,
                                   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Timestamp fulfillFrom,
                                   List<TransactionDto> transactions,
                                   String depositAddress,
                                   Long availableEnergy,
                                   Long availableBandwidth) {
}
