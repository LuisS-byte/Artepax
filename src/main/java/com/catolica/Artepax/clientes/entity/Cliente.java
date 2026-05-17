package com.catolica.Artepax.clientes.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity representing a cliente (customer).
 * <p>
 * Supports soft-delete via the {@code activo} flag: when set to {@code false} the cliente
 * is considered inactive but their historical event data is preserved and still accessible.
 * The {@code correo} field is unique when provided (nullable at the database level).
 * The {@code telefono} field is intentionally NOT unique — family members may share a phone number.
 * </p>
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    /** the cliente's full name */
    @Column(name = "cli_nombre", nullable = false, length = 100)
    private String nombre;

    /** the cliente's phone number, NOT unique (family members share numbers) */
    @Column(name = "cli_telefono", length = 20)
    private String telefono;

    /** the cliente's email address, unique when provided (nullable) */
    @Column(name = "cli_correo", length = 100)
    private String correo;

    /** the cliente's physical address */
    @Column(name = "cli_direccion", length = 255)
    private String direccion;

    /**
     * soft-delete flag: {@code false} means inactive but event history is preserved,
     * {@code true} (default) means active
     */
    @Column(name = "cli_activo", nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "cli_fechacreacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Cliente() {}

    public Long getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
