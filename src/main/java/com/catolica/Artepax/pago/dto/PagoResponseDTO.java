package com.catolica.Artepax.pago.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PagoResponseDTO {

    private Long id;
    private BigDecimal monto;
    private LocalDateTime fecha;
    private Long eventoId;
    private String estadoPago;
    private String conceptoPago;
    private String metodoPago;
    private String clienteNombre;
    private String estadoEventoNombre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public String getConceptoPago() { return conceptoPago; }
    public void setConceptoPago(String conceptoPago) { this.conceptoPago = conceptoPago; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getEstadoEventoNombre() { return estadoEventoNombre; }
    public void setEstadoEventoNombre(String estadoEventoNombre) { this.estadoEventoNombre = estadoEventoNombre; }
}
