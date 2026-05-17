package com.catolica.Artepax.authentication.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new local user.
 *
 * @param nombre   the user's full name
 * @param correo   the user's email address
 * @param password the password (BCrypt-encoded before persistence)
 * @param telefono optional phone number
 * @param rolId    the role identifier, must reference an existing {@code Rol.idRol}
 */
public record UsuarioCreateRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 100) String correo,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 20) String telefono,
        @NotNull Integer rolId
) {}
