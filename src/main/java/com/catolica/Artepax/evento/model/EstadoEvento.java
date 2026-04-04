package com.catolica.Artepax.evento.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estado_evento")
public class EstadoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estadoevento")
    private Long id;

    @Column(name = "estado_nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "estado_descripcion", length = 255)
    private String descripcion;

    public EstadoEvento() {}

    public Long getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

}
