package com.catolica.Artepax.inventario.decoracion.dto;

public class DecoracionProductoResponseDTO {
    private Long productoId;
    private String productoNombre;
    private Integer cantidadRequerida;

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }

    public Integer getCantidadRequerida() { return cantidadRequerida; }
    public void setCantidadRequerida(Integer cantidadRequerida) { this.cantidadRequerida = cantidadRequerida; }
}
