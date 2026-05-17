package com.catolica.Artepax.documento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a document template is requested but does not exist in the database.
 * <p>
 * Maps to an HTTP {@code 404 Not Found} response.
 * </p>
 */
public class PlantillaDocumentoNotFoundException extends ArtepaxException {

    /**
     * Creates an exception for the given template ID.
     *
     * @param id the template ID that was not found
     */
    public PlantillaDocumentoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Plantilla no encontrada: " + id);
    }
}
