package com.catolica.Artepax.pago.entity;

import jakarta.persistence.*;

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
