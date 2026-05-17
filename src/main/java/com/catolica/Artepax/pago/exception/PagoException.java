package com.catolica.Artepax.pago.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all payment-related errors in the pago module.
 * <p>
 * Provides convenience constructors that default to {@link HttpStatus#BAD_REQUEST}
 * when no explicit status is given. Subclasses use specific HTTP status codes
 * to produce consistent error responses via the global exception handler.
 */
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
