package com.catolica.Artepax.inventario.producto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for returning {@code Producto} data in API responses.
 * Includes all product fields: id, name, description, total quantity, unit price,
 * minimum stock threshold, state, and creation timestamp.
 */
public class ProductoResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer cantidadTotal;
    private BigDecimal precioUnitario;
    private Integer stockMinimo;
    private String estado;
    private LocalDateTime fechaCreacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCantidadTotal() { return cantidadTotal; }
    public void setCantidadTotal(Integer cantidadTotal) { this.cantidadTotal = cantidadTotal; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
