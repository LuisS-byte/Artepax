package com.catolica.Artepax.pago.dto;

import java.math.BigDecimal;

/**
 * DTO aggregating the full payment summary for a single event.
 * <p>
 * Combines financial figures from {@code vista_pagos_evento} with
 * advance-payment (anticipo) information for the event's payment
 * overview endpoint.
 */
public class ResumenPagoEventoDTO {
    private Long idEvento;
    private BigDecimal precioTotal;
    private BigDecimal totalCobrado;
    private BigDecimal saldoPendiente;
    private Boolean cobroCompleto;
    private BigDecimal totalDevolucionPendiente;
    private Boolean tieneDevolucionPendiente;
    private AnticipoInfoDTO anticipo;

    public ResumenPagoEventoDTO(Long idEvento, BigDecimal precioTotal,
                                BigDecimal totalCobrado, BigDecimal saldoPendiente,
                                Boolean cobroCompleto,
                                BigDecimal totalDevolucionPendiente,
                                Boolean tieneDevolucionPendiente) {
        this.idEvento = idEvento;
        this.precioTotal = precioTotal;
        this.totalCobrado = totalCobrado;
        this.saldoPendiente = saldoPendiente;
        this.cobroCompleto = cobroCompleto;
        this.totalDevolucionPendiente = totalDevolucionPendiente;
        this.tieneDevolucionPendiente = tieneDevolucionPendiente;
    }

    public ResumenPagoEventoDTO(Long idEvento, BigDecimal precioTotal,
                                BigDecimal totalCobrado, BigDecimal saldoPendiente,
                                Boolean cobroCompleto,
                                BigDecimal totalDevolucionPendiente,
                                Boolean tieneDevolucionPendiente,
                                AnticipoInfoDTO anticipo) {
        this.idEvento = idEvento;
        this.precioTotal = precioTotal;
        this.totalCobrado = totalCobrado;
        this.saldoPendiente = saldoPendiente;
        this.cobroCompleto = cobroCompleto;
        this.totalDevolucionPendiente = totalDevolucionPendiente;
        this.tieneDevolucionPendiente = tieneDevolucionPendiente;
        this.anticipo = anticipo;
    }

    // Getters
    public Long getIdEvento() { return idEvento; }
    public BigDecimal getPrecioTotal() { return precioTotal; }
    public BigDecimal getTotalCobrado() { return totalCobrado; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public Boolean getCobroCompleto() { return cobroCompleto; }
    public BigDecimal getTotalDevolucionPendiente() { return totalDevolucionPendiente; }
    public Boolean getTieneDevolucionPendiente() { return tieneDevolucionPendiente; }
    public AnticipoInfoDTO getAnticipo() { return anticipo; }
}
