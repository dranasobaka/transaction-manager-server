package io.transatron.transaction.manager.domain.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends TransaTronException {

    public ResourceNotFoundException(final String msg) {
        super(msg);
    }

    public ResourceNotFoundException(final String msg, final ErrorsTable error) {
        super(msg, error);
    }

    public ResourceNotFoundException(final String msg, final Exception parentEx) {
        super(msg, parentEx);
    }

    public ResourceNotFoundException(final String msg, final ErrorsTable error, final Exception parentEx) {
        super(msg, error, parentEx);
    }

}
