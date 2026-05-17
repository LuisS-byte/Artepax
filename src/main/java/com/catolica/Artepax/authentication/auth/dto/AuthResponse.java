package com.catolica.Artepax.authentication.auth.dto;

import lombok.*;

/**
 * Response DTO returned by authentication endpoints.
 * <p>
 * Contains the signed JWT string that the client must include
 * in the {@code Authorization: Bearer <token>} header for subsequent requests.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {

    /** The signed JWT token string. */
    private String token;

}