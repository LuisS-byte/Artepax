package com.catolica.Artepax.notificacion.entity;

import jakarta.persistence.*;

/**
 * Catálogo de estados de notificación.
 * <p>
 * Los valores esperados en la base de datos son:
 * <ul>
 *   <li>{@code PENDIENTE} — notificación creada y pendiente de envío o lectura</li>
 *   <li>{@code ENVIADA} — notificación enviada al destino</li>
 *   <li>{@code FALLIDA} — el envío de la notificación falló</li>
 *   <li>{@code LEIDA} — el usuario destino ya leyó la notificación</li>
 * </ul>
 */
@Entity
@Table(name = "estado_notificacion")
public class EstadoNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_notificacion")
    private Long id;

    @Column(name = "nombre_estado", nullable = false, length = 20)
    private String nombreEstado;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    public EstadoNotificacion() {}

    public Long getId() { return id; }

    public String getNombreEstado() { return nombreEstado; }
    public void setNombreEstado(String nombreEstado) { this.nombreEstado = nombreEstado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
