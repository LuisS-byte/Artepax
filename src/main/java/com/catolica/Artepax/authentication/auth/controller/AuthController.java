package com.catolica.Artepax.authentication.auth.controller;

import com.catolica.Artepax.authentication.auth.dto.AuthResponse;
import com.catolica.Artepax.authentication.auth.dto.GoogleAuthRequest;
import com.catolica.Artepax.authentication.auth.dto.LoginRequest;
import com.catolica.Artepax.authentication.auth.service.GoogleAuthService;
import com.catolica.Artepax.authentication.auth.service.LocalAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * <p>
 * Provides two login mechanisms:
 * <ul>
 *   <li>{@code POST /auth/google} &mdash; Google OAuth authentication</li>
 *   <li>{@code POST /auth/login} &mdash; Local email+password authentication</li>
 * </ul>
 * Both endpoints return an {@link AuthResponse} containing a JWT token.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final LocalAuthService localAuthService;

    /**
     * Authenticates a user via Google OAuth.
     * <p>
     * Delegates to {@link GoogleAuthService#authenticateWithGoogle(GoogleAuthRequest)}
     * to verify the Google ID token and returns a signed JWT.
     *
     * @param request the Google auth request containing the ID token
     * @return 200 OK with {@link AuthResponse} containing the JWT
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {

        AuthResponse response = googleAuthService.authenticateWithGoogle(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Local login endpoint (email + password).
     * <p>
     * Validates the request body via {@code @Valid}, delegates to
     * {@link LocalAuthService#login(LoginRequest)}, and returns
     * the JWT token on success.
     *
     * @param request the login request with email and password
     * @return 200 OK with {@link AuthResponse} containing the JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginLocal(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(localAuthService.login(request));
    }
}