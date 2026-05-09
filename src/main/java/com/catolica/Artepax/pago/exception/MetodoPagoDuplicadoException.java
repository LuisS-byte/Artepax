package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

public class MetodoPagoDuplicadoException extends PagoException {

    public MetodoPagoDuplicadoException(String nombre) {
        super(HttpStatus.CONFLICT, "Ya existe un método de pago con el nombre: " + nombre);
    }
}
