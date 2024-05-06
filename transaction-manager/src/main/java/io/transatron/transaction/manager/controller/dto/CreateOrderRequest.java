package io.transatron.transaction.manager.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;
import java.util.List;

public record CreateOrderRequest(List<String> userTransactions,
                                 Long energy,
                                 Long bandwidth,
                                 String paymentTransaction,
                                 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Timestamp fulfillFrom) {
}
