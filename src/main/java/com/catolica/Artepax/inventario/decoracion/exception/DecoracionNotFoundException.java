package com.catolica.Artepax.inventario.decoracion.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class DecoracionNotFoundException extends ArtepaxException {
    public DecoracionNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Decoración no encontrada con ID: " + id);
    }
}

