package com.catolica.Artepax.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base abstracta para todas las excepciones de dominio de Artepax.
 * <p>
 * Cada subclase concreta debe proporcionar un {@link HttpStatus} y
 * opcionalmente un {@code errorCode} que {@link GlobalExceptionHandler}
 * traduce a una {@link ErrorResponse} uniforme para el cliente HTTP.
 * <p>
 * Sigue el patrón: excepción de dominio concreta {@literal →} ArtepaxException
 * {@literal →} GlobalExceptionHandler {@literal →} {@code 4xx/5xx} JSON.
 */
public abstract class ArtepaxException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    /**
     * Crea una excepción con status HTTP y mensaje.
     *
     * @param status  el status HTTP que se devolverá al cliente
     * @param message descripción del error
     */
    protected ArtepaxException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    /**
     * Crea una excepción con status HTTP, mensaje y código de error.
     *
     * @param status    el status HTTP que se devolverá al cliente
     * @param message   descripción del error
     * @param errorCode código alfanumérico para identificar el tipo de error
     */
    protected ArtepaxException(HttpStatus status, String message, String errorCode) {
        this(status, message, errorCode, null);
    }

    /**
     * Crea una excepción con status HTTP, mensaje y causa.
     *
     * @param status  el status HTTP que se devolverá al cliente
     * @param message descripción del error
     * @param cause   la causa raíz del error
     */
    protected ArtepaxException(HttpStatus status, String message, Throwable cause) {
        this(status, message, null, cause);
    }

    /**
     * Crea una excepción con todos los campos.
     *
     * @param status    el status HTTP que se devolverá al cliente
     * @param message   descripción del error
     * @param errorCode código alfanumérico para identificar el tipo de error
     * @param cause     la causa raíz del error
     */
    protected ArtepaxException(HttpStatus status, String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    /**
     * Retorna el status HTTP asociado a esta excepción.
     *
     * @return {@link HttpStatus} que {@link GlobalExceptionHandler} usará como código de respuesta
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Retorna el código de error alfanumérico, si existe.
     *
     * @return el errorCode, o {@code null} si no se especificó
     */
    public String getErrorCode() {
        return errorCode;
    }
}
