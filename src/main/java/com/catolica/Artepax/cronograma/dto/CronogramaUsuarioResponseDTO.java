package com.catolica.Artepax.cronograma.dto;

import java.math.BigDecimal;

public class CronogramaUsuarioResponseDTO {

    private Long cronogramaId;
    private Long usuarioId;
    private String usuarioNombre;
    private BigDecimal pagoAsignado;
    private String pagoTipo;

    public Long getCronogramaId() { return cronogramaId; }
    public void setCronogramaId(Long cronogramaId) { this.cronogramaId = cronogramaId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public BigDecimal getPagoAsignado() { return pagoAsignado; }
    public void setPagoAsignado(BigDecimal pagoAsignado) { this.pagoAsignado = pagoAsignado; }

    public String getPagoTipo() { return pagoTipo; }
    public void setPagoTipo(String pagoTipo) { this.pagoTipo = pagoTipo; }
}
