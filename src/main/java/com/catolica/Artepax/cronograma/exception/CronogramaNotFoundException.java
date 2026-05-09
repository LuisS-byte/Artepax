package com.catolica.Artepax.cronograma.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando no se encuentra una actividad del cronograma.
 */
public class CronogramaNotFoundException extends CronogramaException {

    public CronogramaNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Cronograma no encontrado con id: " + id);
    }

    public CronogramaNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, message, cause);
    }
}
