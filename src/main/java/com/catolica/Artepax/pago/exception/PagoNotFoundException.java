package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a payment record is not found by its ID.
 */
public class PagoNotFoundException extends PagoException {

    /**
     * @param id the payment ID that was not found
     */
    public PagoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Pago no encontrado: " + id);
    }
}
