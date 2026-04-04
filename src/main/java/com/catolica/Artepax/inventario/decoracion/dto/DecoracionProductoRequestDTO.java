package com.catolica.Artepax.inventario.decoracion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
public class DecoracionProductoRequestDTO {

    @NotNull(message = "El producto es obligatorio")
private Long productoId;

    @NotNull(message = "La cantidad requerida es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidadRequerida;

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Integer getCantidadRequerida() { return cantidadRequerida; }
    public void setCantidadRequerida(Integer cantidadRequerida) { this.cantidadRequerida = cantidadRequerida; }
}
