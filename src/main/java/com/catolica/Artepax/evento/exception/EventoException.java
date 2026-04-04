package com.catolica.Artepax.evento.exception;

public class EventoException extends RuntimeException {

    public EventoException(String message) {
        super(message);
    }

    public EventoException(String message, Throwable cause) {
        super(message, cause);
    }
}