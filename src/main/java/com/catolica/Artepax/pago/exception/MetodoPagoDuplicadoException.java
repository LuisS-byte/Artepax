package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to create a payment method with a name
 * that already exists in the catalog (case-insensitive duplicate check).
 */
public class MetodoPagoDuplicadoException extends PagoException {

    /**
     * @param nombre the duplicate payment method name
     */
    public MetodoPagoDuplicadoException(String nombre) {
        super(HttpStatus.CONFLICT, "Ya existe un método de pago con el nombre: " + nombre);
    }
}
