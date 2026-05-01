package com.catolica.Artepax.evento.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gasto_evento")
public class GastoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(name = "gasto_concepto", nullable = false, length = 100)
    private String gastoConcepto;

    @Column(name = "gasto_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal gastoMonto;

    @Column(name = "gasto_estado", nullable = false, length = 15)
    private String gastoEstado = "PROYECTADO";

    @Column(name = "gasto_fecha", nullable = false)
    @CreationTimestamp
    private LocalDateTime gastoFecha;

    @Column(name = "gasto_nota", length = 255)
    private String gastoNota;

    public GastoEvento() {}

    public Long getId() { return id; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public String getGastoConcepto() { return gastoConcepto; }
    public void setGastoConcepto(String gastoConcepto) { this.gastoConcepto = gastoConcepto; }

    public BigDecimal getGastoMonto() { return gastoMonto; }
    public void setGastoMonto(BigDecimal gastoMonto) { this.gastoMonto = gastoMonto; }

    public String getGastoEstado() { return gastoEstado; }
    public void setGastoEstado(String gastoEstado) { this.gastoEstado = gastoEstado; }

    public LocalDateTime getGastoFecha() { return gastoFecha; }
    public void setGastoFecha(LocalDateTime gastoFecha) { this.gastoFecha = gastoFecha; }

    public String getGastoNota() { return gastoNota; }
    public void setGastoNota(String gastoNota) { this.gastoNota = gastoNota; }
}
