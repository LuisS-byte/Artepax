package com.catolica.Artepax.inventario.decoracion.entity;


import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "decoracion")
public class Decoracion {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_decoracion")
    private Long id;

    @Column(name = "deco_nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "decor_descripcion", length = 255)
    private String descripcion;

    @Column(name = "decor_precio", precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "decor_estado", length = 50)
    private String estado;

    @Column(name = "decor_creacion")
    private LocalDate fechaCreacion;

    public Decoracion() {}

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDate.now();
        }
    }

    public Long getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}