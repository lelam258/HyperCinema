package com.cinema.hyperCinema.exception.branch;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BranchNotFoundException extends BranchException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String KEY = "branch.not_found";

    public BranchNotFoundException() {
        super(KEY);
    }

    public BranchNotFoundException(Integer branchId) {
        super(KEY, branchId);
    }

    public BranchNotFoundException(Integer branchId, Throwable cause) {
        super(KEY, cause);
    }
}
