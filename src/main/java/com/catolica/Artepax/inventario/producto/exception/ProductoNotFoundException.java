package com.catolica.Artepax.inventario.producto.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a product is not found by its ID.
 * Returns HTTP 404 Not Found.
 */
public class ProductoNotFoundException extends ArtepaxException {
    public ProductoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Producto no encontrado con ID: " + id);
    }
}
