package com.catolica.Artepax.evento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio para operaciones de tipo de evento.
 * Se lanza cuando un tipo de evento no puede crearse, activarse o desactivarse
 * por reglas de negocio (nombre duplicado, tipo inexistente, etc.).
 */
public class TipoEventoException extends ArtepaxException {

    public TipoEventoException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public TipoEventoException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    protected TipoEventoException(HttpStatus status, String message) {
        super(status, message);
    }

    protected TipoEventoException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
