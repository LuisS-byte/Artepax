package com.catolica.Artepax.clientes.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class ClienteNotFoundException extends ArtepaxException {

    public ClienteNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Cliente no encontrado con id: " + id);
    }

    public ClienteNotFoundException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
