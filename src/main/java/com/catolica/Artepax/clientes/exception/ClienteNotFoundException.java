package com.catolica.Artepax.clientes.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested cliente is not found.
 * <p>
 * Extends {@link ArtepaxException} with HTTP 404 (NOT_FOUND) status.
 * Provides convenience constructors for lookup-by-ID and custom messages.
 * </p>
 */
public class ClienteNotFoundException extends ArtepaxException {

    /**
     * Constructs a new exception with a message including the missing cliente's ID.
     *
     * @param id the ID of the cliente that was not found
     */
    public ClienteNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Cliente no encontrado con id: " + id);
    }

    /**
     * Constructs a new exception with a custom message.
     *
     * @param mensaje the detail message
     */
    public ClienteNotFoundException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
