package com.catolica.Artepax.evento.exception;

import org.springframework.http.HttpStatus;

public class GastoEventoNotFoundException extends GastoEventoException {

    public GastoEventoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Gasto no encontrado: " + id);
    }
}
