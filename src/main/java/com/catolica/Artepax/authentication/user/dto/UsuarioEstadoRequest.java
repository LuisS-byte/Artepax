package com.catolica.Artepax.authentication.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for toggling a user's active state.
 *
 * @param activo {@code true} to activate, {@code false} to deactivate
 */
public record UsuarioEstadoRequest(@NotNull Boolean activo) {}
