package com.cinema.hyperCinema.exception.feedback;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FeedbackNotFoundException extends FeedbackException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FeedbackNotFoundException(String key) {
        super(key);
    }

    public FeedbackNotFoundException(String key, Object... args) {
        super(key, args);
    }
}
