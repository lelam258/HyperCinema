package com.cinema.hyperCinema.exception.movie;

import java.io.Serial;
import org.springframework.http.HttpStatus;

public abstract class MovieException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String key;
    private final transient Object[] args;
    private final HttpStatus httpStatus;

    protected MovieException(String key, HttpStatus httpStatus) {
        super(key);
        this.key = key;
        this.httpStatus = httpStatus;
        this.args = new Object[0];
    }

    protected MovieException(String key, HttpStatus httpStatus, Object... args) {
        super(key);
        this.key = key;
        this.httpStatus = httpStatus;
        this.args = args == null ? new Object[0] : args.clone();
    }

    protected MovieException(String key, HttpStatus httpStatus, Throwable cause) {
        super(key, cause);
        this.key = key;
        this.httpStatus = httpStatus;
        this.args = new Object[0];
    }

    public String getKey() {
        return key;
    }

    public Object[] getArgs() {
        return args.clone();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
