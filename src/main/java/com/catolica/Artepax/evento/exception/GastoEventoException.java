package com.catolica.Artepax.evento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio para operaciones de gasto de evento.
 * <p>
 * Se lanza cuando un gasto no puede crearse, desembolsarse o eliminarse
 * por reglas de negocio (estado inválido, gasto ya desembolsado, etc.).
 */
public class GastoEventoException extends ArtepaxException {

    public GastoEventoException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public GastoEventoException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    protected GastoEventoException(HttpStatus status, String message) {
        super(status, message);
    }

    protected GastoEventoException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
