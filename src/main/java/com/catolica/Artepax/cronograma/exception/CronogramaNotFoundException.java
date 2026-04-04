package com.catolica.Artepax.cronograma.exception;

public class CronogramaNotFoundException extends RuntimeException {

    public CronogramaNotFoundException(Long id) {
        super("Cronograma no encontrado con id: " + id);
    }
}
