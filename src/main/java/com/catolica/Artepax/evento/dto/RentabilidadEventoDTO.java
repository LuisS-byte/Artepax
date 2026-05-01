package com.catolica.Artepax.evento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RentabilidadEventoDTO {

    private Long idEvento;
    private LocalDate fechaEvento;
    private String estado;
    private BigDecimal precioAcordado;
    private BigDecimal ingresoCobrado;
    private BigDecimal costoMateriales;
    private BigDecimal costoManoObra;
    private BigDecimal gastosDesembolsados;
    private BigDecimal gastosProyectados;
    private BigDecimal gananciaNeta;

    public RentabilidadEventoDTO(Long idEvento, LocalDate fechaEvento, String estado,
                                 BigDecimal precioAcordado, BigDecimal ingresoCobrado,
                                 BigDecimal costoMateriales, BigDecimal costoManoObra,
                                 BigDecimal gastosDesembolsados, BigDecimal gastosProyectados,
                                 BigDecimal gananciaNeta) {
        this.idEvento = idEvento;
        this.fechaEvento = fechaEvento;
        this.estado = estado;
        this.precioAcordado = precioAcordado;
        this.ingresoCobrado = ingresoCobrado;
        this.costoMateriales = costoMateriales;
        this.costoManoObra = costoManoObra;
        this.gastosDesembolsados = gastosDesembolsados;
        this.gastosProyectados = gastosProyectados;
        this.gananciaNeta = gananciaNeta;
    }

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
