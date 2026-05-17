package com.catolica.Artepax.pago.dto;

/**
 * DTO representing a payment state from the catalog.
 * <p>
 * Used in read-only responses for the {@code /api/catalogos/estados-pago} endpoint.
 */
public class EstadoPagoResponseDTO {
    private Long id;
    private String estadoNombre;
    private String estadoDescripcion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEstadoNombre() { return estadoNombre; }
    public void setEstadoNombre(String estadoNombre) { this.estadoNombre = estadoNombre; }

    public String getEstadoDescripcion() { return estadoDescripcion; }
    public void setEstadoDescripcion(String estadoDescripcion) { this.estadoDescripcion = estadoDescripcion; }
}
