package com.catolica.Artepax.documento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a document operation violates a business rule or policy.
 * <p>
 * Examples include: attempting to generate a duplicate document, re-sending
 * beyond the maximum allowed attempts, or generating a document for an
 * event in an incompatible state. Maps to HTTP {@code 422 Unprocessable Entity}.
 * </p>
 */
public class DocumentoOperacionNoPermitidaException extends ArtepaxException {

    /**
     * Creates an exception with a descriptive message.
     *
     * @param message the reason the operation is not permitted
     */
    public DocumentoOperacionNoPermitidaException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
