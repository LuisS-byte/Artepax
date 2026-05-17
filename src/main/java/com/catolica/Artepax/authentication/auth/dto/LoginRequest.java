package com.catolica.Artepax.authentication.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for local login requests (email + password).
 * <p>
 * Validation is enforced by Spring's {@link org.springframework.web.bind.MethodArgumentNotValidException}
 * handler via {@code @Valid} on the controller method parameter.
 */
@Data
public class LoginRequest {

    /** The user's email address — must be a valid email format and not blank. */
    @NotBlank
    @Email
    private String email;

    /** The user's password — must not be blank. */
    @NotBlank
    private String password;
}
