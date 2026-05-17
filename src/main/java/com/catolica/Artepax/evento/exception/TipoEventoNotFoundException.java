package com.catolica.Artepax.evento.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando un tipo de evento no es encontrado por su identificador.
 * Retorna HTTP 404 (NOT_FOUND).
 */
public class TipoEventoNotFoundException extends TipoEventoException {

    public TipoEventoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Tipo de evento no encontrado: " + id);
    }
}
