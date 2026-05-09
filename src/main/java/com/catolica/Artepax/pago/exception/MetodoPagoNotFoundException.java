package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

public class MetodoPagoNotFoundException extends PagoException {

    public MetodoPagoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Método de pago no encontrado: " + id);
    }
}
