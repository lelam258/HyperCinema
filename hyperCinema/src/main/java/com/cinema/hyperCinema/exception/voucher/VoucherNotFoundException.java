package com.cinema.hyperCinema.exception.voucher;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class VoucherNotFoundException extends VoucherException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String KEY = "voucher.not_found";

    public VoucherNotFoundException() {
        super(KEY, HttpStatus.NOT_FOUND);
    }

    public VoucherNotFoundException(Integer voucherId) {
        super(KEY, HttpStatus.NOT_FOUND, voucherId);
    }

    public VoucherNotFoundException(Integer voucherId, Throwable cause) {
        super(KEY, HttpStatus.NOT_FOUND, cause);
    }
}
