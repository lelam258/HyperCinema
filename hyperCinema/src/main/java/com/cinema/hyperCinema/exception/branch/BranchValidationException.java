package com.cinema.hyperCinema.exception.branch;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BranchValidationException extends BranchException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BranchValidationException(String key) {
        super(key);
    }

    public BranchValidationException(String key, Object... args) {
        super(key, args);
    }

    public BranchValidationException(String key, Throwable cause) {
        super(key, cause);
    }
}
