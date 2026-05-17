package com.catolica.Artepax.pago.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * Read-only entity mapping to the {@code vista_pagos_evento} database view.
 * <p>
 * This view aggregates payment data per event, computing totals for cobrado,
 * saldo pendiente, cobro completo flag, and devolucion pendiente amounts.
 * The view is refreshed automatically by the database as underlying payments change.
 * <p>
 * Because this entity is {@link Immutable}, Hibernate will never flush updates
 * back to the database. It is used exclusively for querying aggregated payment
 * summaries per event.
 */
@Entity
@Immutable
@Table(name = "vista_pagos_evento")
public class VistaPagosEvento {

    @Id
    @Column(name = "id_evento")
    private Long idEvento;

    @Column(name = "precio_total")
    private BigDecimal precioTotal;

    @Column(name = "total_cobrado")
    private BigDecimal totalCobrado;

    @Column(name = "saldo_pendiente")
    private BigDecimal saldoPendiente;

    @Column(name = "cobro_completo")
    private Boolean cobroCompleto;

    @Column(name = "total_devolucion_pendiente")
    private BigDecimal totalDevolucionPendiente;

    @Column(name = "tiene_devolucion_pendiente")
    private Boolean tieneDevolucionPendiente;

    // Getters
    public Long getIdEvento() { return idEvento; }
    public BigDecimal getPrecioTotal() { return precioTotal; }
    public BigDecimal getTotalCobrado() { return totalCobrado; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public Boolean getCobroCompleto() { return cobroCompleto; }
    public BigDecimal getTotalDevolucionPendiente() { return totalDevolucionPendiente; }
    public Boolean getTieneDevolucionPendiente() { return tieneDevolucionPendiente; }
}