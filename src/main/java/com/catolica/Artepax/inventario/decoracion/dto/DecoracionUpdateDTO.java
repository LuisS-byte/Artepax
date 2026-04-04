package com.catolica.Artepax.inventario.decoracion.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
public class DecoracionUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
private String nombre;

    private String descripcion;
    private BigDecimal precio;
    private String estado;
    private List<DecoracionProductoRequestDTO> productos;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public List<DecoracionProductoRequestDTO> getProductos() { return productos; }
    public void setProductos(List<DecoracionProductoRequestDTO> productos) { this.productos = productos; }
}
