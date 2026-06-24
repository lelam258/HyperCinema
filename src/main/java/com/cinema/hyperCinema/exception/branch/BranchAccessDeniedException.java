package com.cinema.hyperCinema.exception.branch;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class BranchAccessDeniedException extends BranchException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String KEY = "branch.access_denied";

    public BranchAccessDeniedException() {
        super(KEY);
    }

    public BranchAccessDeniedException(String key) {
        super(key);
    }

    public BranchAccessDeniedException(String key, Throwable cause) {
        super(key, cause);
    }
}
