package com.catolica.Artepax.documento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Generic exception thrown when document processing (generation, PDF creation,
 * or delivery) fails unexpectedly.
 * <p>
 * Maps to an HTTP {@code 500 Internal Server Error} response. Serves as the
 * base class for more specific processing exceptions such as
 * {@link EmailDeliveryException} and {@link PdfGenerationException}.
 * </p>
 */
public class DocumentoProcessingException extends ArtepaxException {

    /**
     * Creates an exception with a message and the underlying cause.
     *
     * @param message the error description
     * @param cause   the root cause of the failure
     */
    public DocumentoProcessingException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
