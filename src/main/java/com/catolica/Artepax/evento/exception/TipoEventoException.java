package com.catolica.Artepax.evento.exception;

/**
 * Excepción de dominio para operaciones de tipo de evento.
 * Se lanza cuando un tipo de evento no puede crearse, activarse o desactivarse
 * por reglas de negocio (nombre duplicado, tipo inexistente, etc.).
 */
public class TipoEventoException extends RuntimeException {

    public TipoEventoException(String message) {
        super(message);
    }

    public TipoEventoException(String message, Throwable cause) {
        super(message, cause);
    }
}
