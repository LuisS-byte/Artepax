package com.catolica.Artepax.pago.entity;

import jakarta.persistence.*;

/**
 * Catalog entity representing a payment state.
 * <p>
 * Defines the possible states for a {@link Pago}:
 * <ul>
 *   <li>{@code PENDIENTE} — payment registered but not yet confirmed</li>
 *   <li>{@code PAGADO} — payment confirmed and processed</li>
 *   <li>{@code CANCELADO} — payment cancelled (no financial impact)</li>
 *   <li>{@code DEVOLUCION_PENDIENTE} — payment flagged for manual refund processing</li>
 * </ul>
 */
@Entity
@Table(name = "estado_pago")
public class EstadoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_pago")
    private Long id;

    @Column(name = "estado_nombre", nullable = false, length = 20)
    private String estadoNombre;

    @Column(name = "estado_descripcion", length = 255)
    private String estadoDescripcion;

    public EstadoPago() {}

    public Long getId() { return id; }

    public String getEstadoNombre() { return estadoNombre; }
    public void setEstadoNombre(String estadoNombre) { this.estadoNombre = estadoNombre; }

    public String getEstadoDescripcion() { return estadoDescripcion; }
    public void setEstadoDescripcion(String estadoDescripcion) { this.estadoDescripcion = estadoDescripcion; }
}
