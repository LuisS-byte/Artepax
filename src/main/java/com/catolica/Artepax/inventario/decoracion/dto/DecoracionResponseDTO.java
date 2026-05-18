package com.catolica.Artepax.inventario.decoracion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for returning decoration package data in API responses.
 * Includes the decoration metadata and its associated products.
 */
public class DecoracionResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private String imagenUrl;
    private String estado;
    private LocalDateTime fechaCreacion;
    private List<DecoracionProductoResponseDTO> productos;
    private List<TipoEventoSimpleDTO> tiposCompatibles;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public List<DecoracionProductoResponseDTO> getProductos() { return productos; }
    public void setProductos(List<DecoracionProductoResponseDTO> productos) { this.productos = productos; }

    public List<TipoEventoSimpleDTO> getTiposCompatibles() { return tiposCompatibles; }
    public void setTiposCompatibles(List<TipoEventoSimpleDTO> tiposCompatibles) { this.tiposCompatibles = tiposCompatibles; }
}
