package com.catolica.Artepax.inventario.decoracion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
/**
 * DTO for updating an existing decoration package.
 * Carries the modifiable fields: name, description, price, state,
 * and optionally the products that make up the decoration.
 */
public class DecoracionUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
private String nombre;

    private String descripcion;
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precio;
    private String estado;
    @Valid
    private List<DecoracionProductoRequestDTO> productos;

    /** List of tipo-evento IDs for compatibility. Null = keep current compatibilities. */
    private List<Long> tipoEventoIds;

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

    public List<Long> getTipoEventoIds() { return tipoEventoIds; }
    public void setTipoEventoIds(List<Long> tipoEventoIds) { this.tipoEventoIds = tipoEventoIds; }
}
