package com.catolica.Artepax.evento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio para operaciones de evento.
 * <p>
 * Se lanza cuando una transición de estado, validación o consulta
 * de evento no puede completarse.
 */
public class EventoException extends ArtepaxException {

    public EventoException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public EventoException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    protected EventoException(HttpStatus status, String message) {
        super(status, message);
    }

    protected EventoException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
