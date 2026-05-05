package com.catolica.Artepax.cronograma.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * DTO para asignar un usuario a una actividad del cronograma.
 * El tipo de pago debe ser FIJO o POR_HORA.
 */
public class CronogramaUsuarioRequestDTO {

    @NotNull(message = "El usuario es obligatorio")
    private Long usuarioId;

    private BigDecimal pagoAsignado = BigDecimal.ZERO;

    @Pattern(regexp = "^(FIJO|POR_HORA)$", message = "El tipo de pago debe ser FIJO o POR_HORA")
    private String pagoTipo = "FIJO";

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public BigDecimal getPagoAsignado() { return pagoAsignado; }
    public void setPagoAsignado(BigDecimal pagoAsignado) { this.pagoAsignado = pagoAsignado; }

    public String getPagoTipo() { return pagoTipo; }
    public void setPagoTipo(String pagoTipo) { this.pagoTipo = pagoTipo; }
}
