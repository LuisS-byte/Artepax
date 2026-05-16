package com.catolica.Artepax.authentication.auth.service;

import com.catolica.Artepax.authentication.auth.dto.AuthResponse;
import com.catolica.Artepax.authentication.auth.dto.LoginRequest;
import com.catolica.Artepax.authentication.security.JwtService;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for local email+password authentication.
 * <p>
 * Follows the same validation pattern as {@link GoogleAuthService}:
 * find user by email+authType, check estado, check rol, then issue JWT.
 * Uses BCrypt for password verification.
 * <p>
 * The {@code findByCorreoAndTipoAuth(correo, "LOCAL")} query ensures that
 * GOOGLE users can never authenticate through this flow — the filter is
 * applied at the database level.
 */
@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Authenticates a user with email and password.
     * <p>
     * Validation order:
     * <ol>
     *   <li>Find user by email and {@code tipoAuth = "LOCAL"} — empty → 401</li>
     *   <li>Check {@code estado == true} — false → 403</li>
     *   <li>Check {@code rol.nombre == "ADMIN"} — not admin → 403</li>
     *   <li>BCrypt password verification — mismatch → 401</li>
     *   <li>Generate and return JWT</li>
     * </ol>
     *
     * @param request the login request with email and password
     * @return {@link AuthResponse} with a JWT token
     * @throws ResponseStatusException 401 for invalid credentials, 403 for inactive/non-admin
     */
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByCorreoAndTipoAuth(request.getEmail(), "LOCAL")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo");
        }

        if (usuario.getRol() == null || !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        return AuthResponse.builder()
                .token(jwtService.generateToken(usuario))
                .build();
    }
}
