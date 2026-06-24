package com.cinema.hyperCinema.exception.voucher;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VoucherValidationException extends VoucherException {

    @Serial
    private static final long serialVersionUID = 1L;

    public VoucherValidationException(String key) {
        super(key, HttpStatus.BAD_REQUEST);
    }

    public VoucherValidationException(String key, HttpStatus status) {
        super(key, status);
    }

    public VoucherValidationException(String key, Object... args) {
        super(key, HttpStatus.BAD_REQUEST, args);
    }

    public VoucherValidationException(String key, Throwable cause) {
        super(key, HttpStatus.BAD_REQUEST, cause);
    }
}
