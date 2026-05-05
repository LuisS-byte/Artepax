package com.catolica.Artepax.evento.exception;

/**
 * Excepción de dominio para operaciones de evento.
 * <p>
 * Se lanza cuando una transición de estado, validación o consulta
 * de evento no puede completarse.
 */
public class EventoException extends RuntimeException {

    public EventoException(String message) {
        super(message);
    }

    public EventoException(String message, Throwable cause) {
        super(message, cause);
    }
}
