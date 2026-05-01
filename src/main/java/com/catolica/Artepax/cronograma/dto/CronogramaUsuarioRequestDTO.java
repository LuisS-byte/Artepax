package com.catolica.Artepax.cronograma.dto;


import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CronogramaUsuarioRequestDTO {

    @NotNull(message = "El usuario es obligatorio")
    private Long usuarioId;

    private BigDecimal pagoAsignado = BigDecimal.ZERO;

    private String pagoTipo = "FIJO"; // FIJO o POR_HORA

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public BigDecimal getPagoAsignado() { return pagoAsignado; }
    public void setPagoAsignado(BigDecimal pagoAsignado) { this.pagoAsignado = pagoAsignado; }

    public String getPagoTipo() { return pagoTipo; }
    public void setPagoTipo(String pagoTipo) { this.pagoTipo = pagoTipo; }
}