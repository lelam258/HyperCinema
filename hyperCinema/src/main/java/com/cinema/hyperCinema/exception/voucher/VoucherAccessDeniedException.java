package com.cinema.hyperCinema.exception.voucher;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class VoucherAccessDeniedException extends VoucherException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String KEY = "voucher.access_denied";

    public VoucherAccessDeniedException() {
        super(KEY, HttpStatus.FORBIDDEN);
    }

    public VoucherAccessDeniedException(String key) {
        super(key, HttpStatus.FORBIDDEN);
    }

    public VoucherAccessDeniedException(String key, Throwable cause) {
        super(key, HttpStatus.FORBIDDEN, cause);
    }
}
