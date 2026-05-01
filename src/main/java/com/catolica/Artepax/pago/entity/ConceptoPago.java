package com.catolica.Artepax.pago.entity;

import jakarta.persistence.*;

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
