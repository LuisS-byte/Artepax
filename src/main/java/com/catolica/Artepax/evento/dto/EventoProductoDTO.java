package com.catolica.Artepax.evento.dto;

import java.math.BigDecimal;

/**
 * DTO que representa un producto asociado a un evento.
 * <p>
 * En estado COTIZACION proviene de la composicion de la decoracion
 * (sin reserva real). En estados PROGRAMADO en adelante proviene de
 * evento_producto con el estado de reserva efectivo.
 */
public class EventoProductoDTO {

    private Long productoId;
    private String nombre;
    private Integer cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal costoTotal;
    private String estadoReserva;
    private String origen;

    public EventoProductoDTO() {}

    public EventoProductoDTO(Long productoId, String nombre, Integer cantidad,
                             BigDecimal costoUnitario, String estadoReserva, String origen) {
        this.productoId = productoId;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.costoTotal = costoUnitario != null && cantidad != null
                ? costoUnitario.multiply(BigDecimal.valueOf(cantidad))
                : BigDecimal.ZERO;
        this.estadoReserva = estadoReserva;
        this.origen = origen;
    }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
        recalcularCostoTotal();
    }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
        recalcularCostoTotal();
    }

    public BigDecimal getCostoTotal() { return costoTotal; }
    public void setCostoTotal(BigDecimal costoTotal) { this.costoTotal = costoTotal; }

    public String getEstadoReserva() { return estadoReserva; }
    public void setEstadoReserva(String estadoReserva) { this.estadoReserva = estadoReserva; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    private void recalcularCostoTotal() {
        this.costoTotal = costoUnitario != null && cantidad != null
                ? costoUnitario.multiply(BigDecimal.valueOf(cantidad))
                : BigDecimal.ZERO;
    }
}
