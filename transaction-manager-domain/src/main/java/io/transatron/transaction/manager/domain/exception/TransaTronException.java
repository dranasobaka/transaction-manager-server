package io.transatron.transaction.manager.domain.exception;

import lombok.Getter;

import java.sql.Timestamp;
import java.time.Instant;

@Getter
public class TransaTronException extends RuntimeException {

    protected ErrorsTable error;

    protected Timestamp timestamp;

    public TransaTronException(final String msg) {
        super(msg);
        this.timestamp = Timestamp.from(Instant.now());
    }

    public TransaTronException(final String msg, final ErrorsTable error) {
        super(msg);
        this.error = error;
        this.timestamp = Timestamp.from(Instant.now());
    }

    public TransaTronException(final String msg, final Exception parentEx) {
        super(msg, parentEx);
        this.timestamp = Timestamp.from(Instant.now());
    }

    public TransaTronException(final String msg, final ErrorsTable error, final Exception parentEx) {
        super(msg, parentEx);
        this.error = error;
        this.timestamp = Timestamp.from(Instant.now());
    }

}
