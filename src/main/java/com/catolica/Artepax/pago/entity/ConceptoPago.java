package com.catolica.Artepax.pago.entity;

import jakarta.persistence.*;

/**
 * Catalog entity representing a payment concept.
 * <p>
 * Defines the possible concepts for a payment, such as:
 * <ul>
 *   <li>{@code ANTICIPO} — advance payment / deposit</li>
 *   <li>{@code ABONO} — partial payment</li>
 *   <li>{@code SALDO_TOTAL} — remaining balance payment</li>
 *   <li>{@code PENALIZACION} — penalty charge</li>
 * </ul>
 */
@Entity
@Table(name = "concepto_pago")
public class ConceptoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_concepto_pago")
    private Long id;

    @Column(name = "concepto_nombre", nullable = false, length = 30)
    private String conceptoNombre;

    @Column(name = "concepto_descripcion", length = 255)
    private String conceptoDescripcion;

    public ConceptoPago() {}

    public Long getId() { return id; }

    public String getConceptoNombre() { return conceptoNombre; }
    public void setConceptoNombre(String conceptoNombre) { this.conceptoNombre = conceptoNombre; }

    public String getConceptoDescripcion() { return conceptoDescripcion; }
    public void setConceptoDescripcion(String conceptoDescripcion) { this.conceptoDescripcion = conceptoDescripcion; }
}
