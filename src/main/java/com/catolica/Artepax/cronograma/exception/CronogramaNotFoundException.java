package com.catolica.Artepax.cronograma.exception;

/**
 * Excepción lanzada cuando no se encuentra una actividad del cronograma.
 */
public class CronogramaNotFoundException extends RuntimeException {

    public CronogramaNotFoundException(Long id) {
        super("Cronograma no encontrado con id: " + id);
    }

    public CronogramaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
