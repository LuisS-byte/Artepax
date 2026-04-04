package com.catolica.Artepax.inventario.decoracion.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DecoracionResponseDTO {private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private String estado;
    private LocalDate fechaCreacion;
    private List<DecoracionProductoResponseDTO> productos;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public List<DecoracionProductoResponseDTO> getProductos() { return productos; }
    public void setProductos(List<DecoracionProductoResponseDTO> productos) { this.productos = productos; }
}