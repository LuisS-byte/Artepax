package com.catolica.Artepax.documento.entity;

/**
 * States representing the delivery lifecycle of a document.
 * <p>
 * Flow: {@link #PENDIENTE} (initial state after generation) →
 * {@link #ENVIADO} (successfully sent) → may revert to {@link #FALLIDO} on error.
 * {@link #REENVIADO} indicates a subsequent successful resend after a failure.
 * </p>
 */
public enum DocumentoEstadoEnvio {
    /** Document generated but not yet sent. */
    PENDIENTE,
    /** Document sent successfully. */
    ENVIADO,
    /** Delivery failed; allows retry via re-envio. */
    FALLIDO,
    /** Document successfully resent after a previous failure. */
    REENVIADO
}
