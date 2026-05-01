package com.catolica.Artepax.evento.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GastoEventoResponseDTO {
    private Long id;
    private Long eventoId;
    private String gastoConcepto;
    private BigDecimal gastoMonto;
    private String gastoEstado;
    private LocalDateTime gastoFecha;
    private String gastoNota;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

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
