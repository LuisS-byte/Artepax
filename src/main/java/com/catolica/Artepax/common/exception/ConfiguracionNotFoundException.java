package com.catolica.Artepax.common.exception;

import org.springframework.http.HttpStatus;

public class ConfiguracionNotFoundException extends ArtepaxException {

    public ConfiguracionNotFoundException(String clave) {
        super(HttpStatus.NOT_FOUND, "Configuración no encontrada: " + clave);
    }
}
