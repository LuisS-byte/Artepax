package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

public class ConceptoPagoNotFoundException extends PagoException {

    public ConceptoPagoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Concepto de pago no encontrado: " + id);
    }
}
