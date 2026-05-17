package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a payment method (catalog entry) is not found by its ID.
 */
public class MetodoPagoNotFoundException extends PagoException {

    /**
     * @param id the payment method ID that was not found
     */
    public MetodoPagoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Método de pago no encontrado: " + id);
    }
}
