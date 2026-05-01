package com.catolica.Artepax.evento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public class GastoEventoRequestDTO {

    @NotNull(message = "El evento es obligatorio")
    private Long eventoId;

    @NotBlank(message = "El concepto es obligatorio")
    private String gastoConcepto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal gastoMonto;

    private String gastoEstado = "PROYECTADO";
    private String gastoNota;

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public String getGastoConcepto() { return gastoConcepto; }
    public void setGastoConcepto(String gastoConcepto) { this.gastoConcepto = gastoConcepto; }

    public BigDecimal getGastoMonto() { return gastoMonto; }
    public void setGastoMonto(BigDecimal gastoMonto) { this.gastoMonto = gastoMonto; }

    public String getGastoEstado() { return gastoEstado; }
    public void setGastoEstado(String gastoEstado) { this.gastoEstado = gastoEstado; }

    public String getGastoNota() { return gastoNota; }
    public void setGastoNota(String gastoNota) { this.gastoNota = gastoNota; }
}