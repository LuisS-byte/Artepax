package com.catolica.Artepax.documento.exception;

/**
 * Thrown when PDF generation from HTML content fails.
 * <p>
 * This is a specialised {@link DocumentoProcessingException} for failures
 * in the HTML-to-PDF rendering pipeline.
 * </p>
 */
public class PdfGenerationException extends DocumentoProcessingException {

    /**
     * Creates an exception with a message and the underlying cause.
     *
     * @param message the error description
     * @param cause   the root cause of the PDF generation failure
     */
    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
