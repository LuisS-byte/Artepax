package com.catolica.Artepax.evento.exception;

import org.springframework.http.HttpStatus;

public class EventoNotFoundException extends EventoException {

    public EventoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Evento no encontrado: " + id);
    }

    public EventoNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
