package com.catolica.Artepax.evento.exception;

import org.springframework.http.HttpStatus;

public class TipoEventoNotFoundException extends TipoEventoException {

    public TipoEventoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Tipo de evento no encontrado: " + id);
    }
}
