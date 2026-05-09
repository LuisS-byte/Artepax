package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

public class EstadoPagoNoConfiguradoException extends PagoException {

    public EstadoPagoNoConfiguradoException(String estadoNombre) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Estado " + estadoNombre + " no encontrado en BD");
    }
}
