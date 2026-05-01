package com.catolica.Artepax.inventario.decoracion.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "decor_precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "decor_imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "decor_estado", length = 10)
    private String estado;

    @CreationTimestamp
    @Column(name = "decor_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Decoracion() {}

    public Long getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
