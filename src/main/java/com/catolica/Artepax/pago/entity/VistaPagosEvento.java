package com.catolica.Artepax.pago.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

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

    // Getters
    public Long getIdEvento() { return idEvento; }
    public BigDecimal getPrecioTotal() { return precioTotal; }
    public BigDecimal getTotalCobrado() { return totalCobrado; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public Boolean getCobroCompleto() { return cobroCompleto; }
}