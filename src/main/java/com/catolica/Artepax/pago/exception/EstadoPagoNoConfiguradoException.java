package com.catolica.Artepax.pago.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a required payment state (e.g. PENDIENTE, PAGADO, CANCELADO)
 * is missing from the database catalog.
 * <p>
 * This indicates a misconfiguration — the database seed data is incomplete
 * or the application version does not match the database schema.
 */
public class EstadoPagoNoConfiguradoException extends PagoException {

    /**
     * @param estadoNombre the name of the missing state (e.g. "PENDIENTE")
     */
    public EstadoPagoNoConfiguradoException(String estadoNombre) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Estado " + estadoNombre + " no encontrado en BD");
    }
}
