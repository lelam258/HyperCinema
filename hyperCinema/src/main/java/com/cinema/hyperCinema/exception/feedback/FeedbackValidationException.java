package com.cinema.hyperCinema.exception.feedback;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FeedbackValidationException extends FeedbackException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FeedbackValidationException(String key) {
        super(key);
    }

    public FeedbackValidationException(String key, Object... args) {
        super(key, args);
    }
}
