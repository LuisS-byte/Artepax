package com.catolica.Artepax.cronograma.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando no se encuentra una actividad del cronograma.
 * Retorna HTTP 404 (NOT_FOUND).
 */
public class CronogramaNotFoundException extends CronogramaException {

    /**
     * Crea una excepción con el id de la actividad no encontrada.
     *
     * @param id identificador de la actividad buscada
     */
    public CronogramaNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Cronograma no encontrado con id: " + id);
    }

    /**
     * Crea una excepción con mensaje personalizado y causa.
     *
     * @param message descripción del error
     * @param cause   causa original
     */
    public CronogramaNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, message, cause);
    }
}
