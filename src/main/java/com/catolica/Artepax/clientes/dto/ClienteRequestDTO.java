package com.catolica.Artepax.clientes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new cliente.
 * <p>
 * Contains the required fields for cliente creation with Jakarta Validation constraints.
 * All text fields are trimmed and normalized by the service layer before persistence.
 * </p>
 */
public class ClienteRequestDTO {

    /** the cliente's full name, must not be blank and up to 100 characters */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    /** the cliente's phone number, must not be blank and up to 20 characters */
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    /** the cliente's email address, must be a valid email and up to 100 characters */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe ser válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    /** the cliente's physical address, up to 255 characters (optional) */
    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String direccion;

    // 🔹 Getters y Setters

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
}
