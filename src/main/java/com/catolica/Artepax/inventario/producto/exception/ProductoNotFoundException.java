package com.catolica.Artepax.inventario.producto.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class ProductoNotFoundException extends ArtepaxException {
    public ProductoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Producto no encontrado con ID: " + id);
    }
}
