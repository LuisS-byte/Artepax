package com.catolica.Artepax.notificacion.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class NotificacionNotFoundException extends ArtepaxException {

    public NotificacionNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Notificación no encontrada: " + id);
    }
}
