package com.catolica.Artepax.inventario.movimiento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for registering an inventory movement (entry or exit).
 * Carries the product ID, quantity, optional reason, and the movement origin type
 * ({@code REPOSICION}, {@code AJUSTE_MANUAL}, {@code DAÑO}, or {@code PERDIDA}).
 */
public class MovimientoRequestDTO {

    @NotNull(message = "El id del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    private String motivo;

    // REPOSICION, AJUSTE_MANUAL, DAÑO, PERDIDA
    private String tipoOrigen;

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getTipoOrigen() { return tipoOrigen; }
    public void setTipoOrigen(String tipoOrigen) { this.tipoOrigen = tipoOrigen; }
}