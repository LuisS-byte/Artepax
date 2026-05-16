package com.catolica.Artepax.authentication.user.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for user data exposed via the API.
 * <p>
 * Intentionally omits the password field — credentials are never included
 * in API responses.
 *
 * @param id           the user's unique identifier
 * @param nombre       the user's full name
 * @param correo       the user's email address
 * @param telefono     optional phone number
 * @param tipoAuth     authentication type ({@code "LOCAL"} or {@code "GOOGLE"})
 * @param estado       {@code true} if the user is active
 * @param rolId        the role's numeric identifier
 * @param rol          the role's display name
 * @param fechaCreacion timestamp of account creation
 */
public record UsuarioResponseDTO(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String tipoAuth,
        Boolean estado,
        Integer rolId,
        String rol,
        LocalDateTime fechaCreacion
) {}
