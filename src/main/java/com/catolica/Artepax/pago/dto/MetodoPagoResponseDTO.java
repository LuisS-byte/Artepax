package com.catolica.Artepax.pago.dto;

/**
 * DTO representing a payment method from the catalog.
 * <p>
 * Used in read-only responses to display payment method details
 * including its soft-delete activation status.
 */
public class MetodoPagoResponseDTO {
    private Long id;
    private String metodoNombre;
    private String metodoDescripcion;
    private Boolean metodoActivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMetodoNombre() { return metodoNombre; }
    public void setMetodoNombre(String metodoNombre) { this.metodoNombre = metodoNombre; }

    public String getMetodoDescripcion() { return metodoDescripcion; }
    public void setMetodoDescripcion(String metodoDescripcion) { this.metodoDescripcion = metodoDescripcion; }

    public Boolean getMetodoActivo() { return metodoActivo; }
    public void setMetodoActivo(Boolean metodoActivo) { this.metodoActivo = metodoActivo; }
}
