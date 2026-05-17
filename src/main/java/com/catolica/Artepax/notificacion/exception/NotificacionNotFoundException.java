package com.catolica.Artepax.notificacion.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando no se encuentra una notificación por su identificador.
 * <p>
 * Retorna un HTTP {@code 404 NOT FOUND} con un mensaje descriptivo que incluye
 * el ID solicitado.
 */
public class NotificacionNotFoundException extends ArtepaxException {

    public NotificacionNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Notificación no encontrada: " + id);
    }
}
