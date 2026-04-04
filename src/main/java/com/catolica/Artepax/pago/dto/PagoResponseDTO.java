package com.catolica.Artepax.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
public class PagoResponseDTO {private Long id;
    private BigDecimal monto;
    private LocalDate fecha;
    private String metodo;
    private Long eventoId;
    private String estadoPago; // PENDIENTE o PAGADO

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
}
