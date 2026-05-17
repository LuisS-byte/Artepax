package com.catolica.Artepax.evento.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un gasto de evento no es encontrado por su identificador.
 * Retorna HTTP 404 (NOT_FOUND).
 */
public class GastoEventoNotFoundException extends GastoEventoException {

    public GastoEventoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Gasto no encontrado: " + id);
    }
}
