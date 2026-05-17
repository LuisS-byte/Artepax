package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a payment concept (catalog entry) is not found by its ID.
 */
public class ConceptoPagoNotFoundException extends PagoException {

    /**
     * @param id the payment concept ID that was not found
     */
    public ConceptoPagoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Concepto de pago no encontrado: " + id);
    }
}
