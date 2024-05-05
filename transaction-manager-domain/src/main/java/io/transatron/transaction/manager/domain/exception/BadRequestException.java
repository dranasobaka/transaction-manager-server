package io.transatron.transaction.manager.domain.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends TransaTronException {

    public BadRequestException(final String msg) {
        super(msg);
    }

    public BadRequestException(final String msg, final ErrorsTable error) {
        super(msg, error);
    }

    public BadRequestException(final String msg, final Exception parentEx) {
        super(msg, parentEx);
    }

    public BadRequestException(final String msg, final ErrorsTable error, final Exception parentEx) {
        super(msg, error, parentEx);
    }

}
