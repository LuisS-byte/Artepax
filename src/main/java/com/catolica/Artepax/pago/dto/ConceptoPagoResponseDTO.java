package com.catolica.Artepax.pago.dto;

/**
 * DTO representing a payment concept from the catalog.
 * <p>
 * Used in read-only responses for the {@code /api/catalogos/conceptos-pago} endpoint.
 */
public class ConceptoPagoResponseDTO {
    private Long id;
    private String conceptoNombre;
    private String conceptoDescripcion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConceptoNombre() { return conceptoNombre; }
    public void setConceptoNombre(String conceptoNombre) { this.conceptoNombre = conceptoNombre; }

    public String getConceptoDescripcion() { return conceptoDescripcion; }
    public void setConceptoDescripcion(String conceptoDescripcion) { this.conceptoDescripcion = conceptoDescripcion; }
}
