package com.catolica.Artepax.pago.dto;


import java.math.BigDecimal;

public class ResumenPagoEventoDTO {
    private Long idEvento;
    private BigDecimal precioTotal;
    private BigDecimal totalCobrado;
    private BigDecimal saldoPendiente;
    private Boolean cobroCompleto;

    public ResumenPagoEventoDTO(Long idEvento, BigDecimal precioTotal,
                                BigDecimal totalCobrado, BigDecimal saldoPendiente,
                                Boolean cobroCompleto) {
        this.idEvento = idEvento;
        this.precioTotal = precioTotal;
        this.totalCobrado = totalCobrado;
        this.saldoPendiente = saldoPendiente;
        this.cobroCompleto = cobroCompleto;
    }

    // Getters
    public Long getIdEvento() { return idEvento; }
    public BigDecimal getPrecioTotal() { return precioTotal; }
    public BigDecimal getTotalCobrado() { return totalCobrado; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public Boolean getCobroCompleto() { return cobroCompleto; }
}
