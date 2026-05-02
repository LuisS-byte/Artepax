package com.catolica.Artepax.inventario.decoracion.exception;

public class DecoracionNotFoundException extends RuntimeException {
    public DecoracionNotFoundException(Long id) {
        super("Decoración no encontrada con ID: " + id);
    }
}

