package com.catolica.Artepax.pago.entity;

import jakarta.persistence.*;

/**
 * Catalog entity representing a payment method.
 * <p>
 * Stores available payment methods (e.g. cash, transfer, card).
 * Supports soft-delete via the {@link #metodoActivo} flag — inactive methods
 * are not available for new payments but remain on historical records.
 */
@Entity
@Table(name = "metodo_pago")
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo_pago")
    private Long id;

    @Column(name = "metodo_nombre", nullable = false, length = 30)
    private String metodoNombre;

    @Column(name = "metodo_descripcion", length = 255)
    private String metodoDescripcion;

    @Column(name = "metodo_activo", nullable = false)
    private Boolean metodoActivo = true;

    public MetodoPago() {}

    public Long getId() { return id; }

    public String getMetodoNombre() { return metodoNombre; }
    public void setMetodoNombre(String metodoNombre) { this.metodoNombre = metodoNombre; }

    public String getMetodoDescripcion() { return metodoDescripcion; }
    public void setMetodoDescripcion(String metodoDescripcion) { this.metodoDescripcion = metodoDescripcion; }

    public Boolean getMetodoActivo() { return metodoActivo; }
    public void setMetodoActivo(Boolean metodoActivo) { this.metodoActivo = metodoActivo; }
}
