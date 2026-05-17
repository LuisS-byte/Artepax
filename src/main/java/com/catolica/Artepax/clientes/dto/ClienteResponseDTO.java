package com.catolica.Artepax.clientes.dto;

/**
 * Response DTO for cliente data.
 * <p>
 * Represents the full cliente state returned by the API, including the soft-delete status.
 * </p>
 */
public class ClienteResponseDTO {

    private Long id;
    private String nombre;
    private String telefono;
    private String correo;
    private String direccion;
    private Boolean activo;

    // 🔹 Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}