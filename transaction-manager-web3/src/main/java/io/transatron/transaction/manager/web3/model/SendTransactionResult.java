package io.transatron.transaction.manager.web3.model;

public record SendTransactionResult(String txHash,
                                    int exitCode,
                                    String errorMessage) {
}
