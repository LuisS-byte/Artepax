package com.catolica.Artepax.documento.exception;

/**
 * Thrown when the document email cannot be delivered to the client.
 * <p>
 * This is a specialised {@link DocumentoProcessingException} for SMTP or
 * email-sending failures.
 * </p>
 */
public class EmailDeliveryException extends DocumentoProcessingException {

    /**
     * Creates an exception with a message and the underlying cause.
     *
     * @param message the error description
     * @param cause   the root cause of the email delivery failure
     */
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
