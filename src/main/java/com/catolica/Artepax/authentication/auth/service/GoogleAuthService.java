package com.catolica.Artepax.authentication.auth.service;

import com.catolica.Artepax.authentication.config.GoogleConfig;
import com.catolica.Artepax.authentication.security.JwtService;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.service.UsuarioService;

import com.catolica.Artepax.authentication.auth.dto.GoogleAuthRequest;
import com.catolica.Artepax.authentication.auth.dto.AuthResponse;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Service for Google OAuth authentication.
 * <p>
 * Verifies a Google ID token via {@link GoogleConfig#verifyToken(String)},
 * resolves the authenticated {@link Usuario}, and returns a signed JWT.
 * Only active ADMIN users registered with the system are allowed through.
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleConfig googleConfig;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    /**
     * Entry point called by {@link AuthController#loginWithGoogle}.
     * <p>
     * Delegates to {@link #authenticate(String)} and wraps the result.
     *
     * @param request the Google auth request containing the ID token
     * @return {@link AuthResponse} with a signed JWT
     */
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {

        String token = authenticate(request.getIdToken());

        return AuthResponse.builder()
                .token(token)
                .build();
    }

    /**
     * Performs the full Google authentication flow:
     * <ol>
     *   <li>Verifies the ID token with Google's API</li>
     *   <li>Looks up the user by email in the local database</li>
     *   <li>Checks that the user is active</li>
     *   <li>Checks that the user has the ADMIN role</li>
     *   <li>Generates and returns a signed JWT</li>
     * </ol>
     *
     * @param idToken the raw Google ID token string
     * @return a signed JWT for the authenticated user
     * @throws org.springframework.web.server.ResponseStatusException 401 if the token is invalid,
     *                                                                 403 if the user is not found, inactive, or not an ADMIN
     */
    public String authenticate(String idToken) {

        GoogleIdToken.Payload payload = googleConfig.verifyToken(idToken);

        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }

        String email = payload.getEmail().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioService.findByCorreo(email);

        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autorizado");
        }

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo");
        }

        if (usuario.getRol() == null ||
            !usuario.getRol().getNombre().equals("ADMIN")) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos");
        }

        return jwtService.generateToken(usuario);
    }
}