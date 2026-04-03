package com.catolica.Artepax.clientes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClienteNotFoundException extends RuntimeException {

    public ClienteNotFoundException(Long id) {
        super("Cliente no encontrado con id: " + id);
    }

    public ClienteNotFoundException(String mensaje) {
        super(mensaje);
    }
}