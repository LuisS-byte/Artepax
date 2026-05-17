package com.catolica.Artepax.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando se solicita una clave de configuración
 * que no existe en la base de datos.
 * <p>
 * Retorna HTTP 404 ({@link HttpStatus#NOT_FOUND}) con un mensaje
 * que incluye la clave solicitada.
 */
public class ConfiguracionNotFoundException extends ArtepaxException {

    /**
     * Crea una excepción para la clave de configuración faltante.
     *
     * @param clave la clave de configuración que no se encontró
     */
    public ConfiguracionNotFoundException(String clave) {
        super(HttpStatus.NOT_FOUND, "Configuración no encontrada: " + clave);
    }
}
