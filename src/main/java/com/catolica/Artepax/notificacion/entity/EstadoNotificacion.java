package com.catolica.Artepax.notificacion.entity;

import jakarta.persistence.*;

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
