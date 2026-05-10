package com.catolica.Artepax.cronograma.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio para errores de reglas de negocio del cronograma.
 * Reemplaza el uso genérico de IllegalArgumentException en el servicio.
 */
public class CronogramaException extends ArtepaxException {

    public CronogramaException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public CronogramaException(String errorCode, String message) {
        this(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public CronogramaException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    protected CronogramaException(HttpStatus status, String message) {
        super(status, message);
    }

    protected CronogramaException(HttpStatus status, String errorCode, String message) {
        super(status, message, errorCode);
    }

    protected CronogramaException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
