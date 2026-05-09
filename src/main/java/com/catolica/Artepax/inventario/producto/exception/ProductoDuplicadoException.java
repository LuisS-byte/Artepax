package com.catolica.Artepax.inventario.producto.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class ProductoDuplicadoException extends ArtepaxException {
    public ProductoDuplicadoException(String nombre) {
        super(HttpStatus.CONFLICT, "Ya existe un producto con el nombre: " + nombre);
    }
}
