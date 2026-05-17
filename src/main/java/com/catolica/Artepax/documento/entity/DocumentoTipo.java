package com.catolica.Artepax.documento.entity;

/**
 * Types of documents that can be generated for an event.
 * <p>
 * Each type corresponds to a specific document template and is associated
 * with a particular stage or purpose in the event lifecycle.
 * </p>
 */
public enum DocumentoTipo {
    /** Booking confirmation document sent after the event is confirmed. */
    CONFIRMACION_RESERVA,
    /** Quotation document sent during the quotation stage. */
    COTIZACION_EVENTO,
    /** Placeholder type not directly usable; use {@link #AVISO_SALDO_PENDIENTE} instead. */
    SALDO_PENDIENTE,
    /** Outstanding balance notice sent when the client has a pending payment. */
    AVISO_SALDO_PENDIENTE
}
