package com.catolica.Artepax.pago.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class PagoException extends ArtepaxException {

    public PagoException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public PagoException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    protected PagoException(HttpStatus status, String message) {
        super(status, message);
    }

    protected PagoException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
