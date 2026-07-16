package com.cinema.hyperCinema.exception.feedback;

import java.io.Serial;

public abstract class FeedbackException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String key;
    private final transient Object[] args;

    protected FeedbackException(String key) {
        super(key);
        this.key = key;
        this.args = new Object[0];
    }

    protected FeedbackException(String key, Object... args) {
        super(key);
        this.key = key;
        this.args = args == null ? new Object[0] : args.clone();
    }

    protected FeedbackException(String key, Throwable cause) {
        super(key, cause);
        this.key = key;
        this.args = new Object[0];
    }

    public String getKey() {
        return key;
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
