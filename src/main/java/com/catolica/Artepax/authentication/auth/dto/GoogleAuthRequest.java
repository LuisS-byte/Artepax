package com.catolica.Artepax.authentication.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for Google OAuth authentication.
 * <p>
 * Carries the ID token issued by Google's OAuth 2.0 endpoint.
 * The token is verified server-side by {@link com.catolica.Artepax.authentication.config.GoogleConfig}.
 */
@Data
public class GoogleAuthRequest {

    @NotBlank(message = "El idToken es obligatorio")
    private String idToken;

}
