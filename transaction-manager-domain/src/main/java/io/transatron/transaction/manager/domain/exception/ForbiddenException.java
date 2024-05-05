package io.transatron.transaction.manager.domain.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends TransaTronException {

    public ForbiddenException(final String msg) {
        super(msg);
    }

    public ForbiddenException(final String msg, final ErrorsTable error) {
        super(msg, error);
    }

    public ForbiddenException(final String msg, final Exception parentEx) {
        super(msg, parentEx);
    }

    public ForbiddenException(final String msg, final ErrorsTable error, final Exception parentEx) {
        super(msg, error, parentEx);
    }

}
