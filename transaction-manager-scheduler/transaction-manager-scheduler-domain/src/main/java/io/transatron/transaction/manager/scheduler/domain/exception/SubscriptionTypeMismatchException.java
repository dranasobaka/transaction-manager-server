package io.transatron.transaction.manager.scheduler.domain.exception;

public class SubscriptionTypeMismatchException extends RuntimeException {

    public SubscriptionTypeMismatchException(String message) {
        super(message);
    }
}
