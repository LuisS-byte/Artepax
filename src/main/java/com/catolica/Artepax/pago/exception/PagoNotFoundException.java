package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

public class PagoNotFoundException extends PagoException {

    public PagoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Pago no encontrado: " + id);
    }
}
