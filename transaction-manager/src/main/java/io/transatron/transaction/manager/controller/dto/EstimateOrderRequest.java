package io.transatron.transaction.manager.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;
import java.util.List;

public record EstimateOrderRequest(List<String> userTransactions,
                                   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Timestamp fulfillFrom) {
}
