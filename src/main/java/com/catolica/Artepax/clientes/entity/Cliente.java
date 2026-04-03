package com.catolica.Artepax.clientes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @Column(name = "cli_nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "cli_telefono", length = 20)
    private String telefono;

    @Column(name = "cli_direccion", length = 255)
    private String direccion;

    @Column(name = "cli_fechacreacion")
    private LocalDate fechaCreacion;

    @Column(name = "cli_correo", length = 100)
    private String correo;

    // 🔹 Constructor vacío
    public Cliente() {
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    // 🔥 Automático como en la BD
    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDate.now();
        }
    }
}