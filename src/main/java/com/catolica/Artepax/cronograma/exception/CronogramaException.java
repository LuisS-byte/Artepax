package com.catolica.Artepax.cronograma.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Excepción de dominio para errores de reglas de negocio del cronograma.
 * Reemplaza el uso genérico de {@code IllegalArgumentException} en el servicio.
 * Por defecto retorna HTTP 400 (BAD_REQUEST).
 */
public class CronogramaException extends ArtepaxException {

    /**
     * Crea una excepción con mensaje y HTTP 400.
     *
     * @param message descripción del error
     */
    public CronogramaException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Crea una excepción con código de error, mensaje y HTTP 400.
     *
     * @param errorCode código de negocio para el frontend
     * @param message   descripción del error
     */
    public CronogramaException(String errorCode, String message) {
        this(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    /**
     * Crea una excepción con mensaje, causa y HTTP 400.
     *
     * @param message descripción del error
     * @param cause   causa original
     */
    public CronogramaException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    /**
     * Crea una excepción con status HTTP personalizado y mensaje.
     *
     * @param status  status HTTP
     * @param message descripción del error
     */
    protected CronogramaException(HttpStatus status, String message) {
        super(status, message);
    }

    /**
     * Crea una excepción con status HTTP, código de error y mensaje.
     *
     * @param status    status HTTP
     * @param errorCode código de negocio para el frontend
     * @param message   descripción del error
     */
    protected CronogramaException(HttpStatus status, String errorCode, String message) {
        super(status, message, errorCode);
    }

    /**
     * Crea una excepción con status HTTP, mensaje y causa.
     *
     * @param status  status HTTP
     * @param message descripción del error
     * @param cause   causa original
     */
    protected CronogramaException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
