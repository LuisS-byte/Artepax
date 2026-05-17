package com.catolica.Artepax.evento.model;


import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vista de base de datos {@code vista_rentabilidad_evento} que calcula
 * la rentabilidad por evento.
 * <p>
 * Incluye precio acordado, ingreso cobrado, costos de materiales y mano de
 * obra, gastos desembolsados y proyectados, y la ganancia neta. Es de solo
 * lectura ({@link Immutable}).
 */
@Entity
@Immutable
@Table(name = "vista_rentabilidad_evento")
public class VistaRentabilidadEvento {@Id
@Column(name = "id_evento")
private Long idEvento;

    @Column(name = "event_fechaevento")
    private LocalDate fechaEvento;

    @Column(name = "estado")
    private String estado;

    @Column(name = "precio_acordado")
    private BigDecimal precioAcordado;

    @Column(name = "ingreso_cobrado")
    private BigDecimal ingresoCobrado;

    @Column(name = "costo_materiales")
    private BigDecimal costoMateriales;

    @Column(name = "costo_mano_obra")
    private BigDecimal costoManoObra;

    @Column(name = "gastos_desembolsados")
    private BigDecimal gastosDesembolsados;

    @Column(name = "gastos_proyectados")
    private BigDecimal gastosProyectados;

    @Column(name = "ganancia_neta")
    private BigDecimal gananciaNeta;

    // Getters
    public Long getIdEvento() { return idEvento; }
    public LocalDate getFechaEvento() { return fechaEvento; }
    public String getEstado() { return estado; }
    public BigDecimal getPrecioAcordado() { return precioAcordado; }
    public BigDecimal getIngresoCobrado() { return ingresoCobrado; }
    public BigDecimal getCostoMateriales() { return costoMateriales; }
    public BigDecimal getCostoManoObra() { return costoManoObra; }
    public BigDecimal getGastosDesembolsados() { return gastosDesembolsados; }
    public BigDecimal getGastosProyectados() { return gastosProyectados; }
    public BigDecimal getGananciaNeta() { return gananciaNeta; }
}