package com.catolica.Artepax.cronograma.exception;

/**
 * Excepción de dominio para errores de reglas de negocio del cronograma.
 * Reemplaza el uso genérico de IllegalArgumentException en el servicio.
 */
public class CronogramaException extends RuntimeException {

    public CronogramaException(String message) {
        super(message);
    }

    public CronogramaException(String message, Throwable cause) {
        super(message, cause);
    }
}
