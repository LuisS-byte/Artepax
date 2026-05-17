package com.catolica.Artepax.documento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a document is requested but does not exist in the database.
 * <p>
 * Maps to an HTTP {@code 404 Not Found} response.
 * </p>
 */
public class DocumentoNotFoundException extends ArtepaxException {

    /**
     * Creates an exception for the given document ID.
     *
     * @param id the document ID that was not found
     */
    public DocumentoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Documento no encontrado: " + id);
    }
}
