package com.catolica.Artepax.inventario.producto.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @Column(name = "produ_nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "produ_descripcion", length = 255)
    private String descripcion;

    @Column(name = "produ_cantidad_total", nullable = false)
    private Integer cantidadTotal;

    @Column(name = "produ_estado", length = 50)
    private String estado;

    @Column(name = "produ_creacion")
    private LocalDate fechaCreacion;

    // 🔹 Constructor vacío (JPA obligatorio)
    public Producto() {
    }

    // 🔹 Getters y Setters

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidadTotal() {
        return cantidadTotal;
    }

    public void setCantidadTotal(Integer cantidadTotal) {
        this.cantidadTotal = cantidadTotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // 🔥 Se asigna automáticamente como en la BD
    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDate.now();
        }
    }
}