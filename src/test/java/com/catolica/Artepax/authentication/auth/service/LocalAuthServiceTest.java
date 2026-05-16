package com.catolica.Artepax.authentication.auth.service;

import com.catolica.Artepax.authentication.auth.dto.AuthResponse;
import com.catolica.Artepax.authentication.auth.dto.LoginRequest;
import com.catolica.Artepax.authentication.security.JwtService;
import com.catolica.Artepax.authentication.user.entity.Rol;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LocalAuthService}.
 * <p>
 * Covers all authentication flows per the {@code login-local} spec:
 * success, wrong password, unknown email, GOOGLE user attempt,
 * inactive user, non-ADMIN role, and non-BCrypt password hash.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocalAuthService unit tests")
class LocalAuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LocalAuthService localAuthService;

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private Usuario buildUsuario(String password, boolean estado, String rolNombre) {
        Rol rol = (rolNombre != null)
                ? Rol.builder().idRol(1).nombre(rolNombre).descripcion("test role").build()
                : null;
        return Usuario.builder()
                .idUsuario(1L)
                .nombre("Admin Test")
                .correo("admin@artepax.com")
                .password(password)
                .tipoAuth("LOCAL")
                .estado(estado)
                .rol(rol)
                .build();
    }

    private LoginRequest buildRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    // -------------------------------------------------------------------------
    // 1. Successful login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Successful login with valid LOCAL credentials")
    void login_success() {
        String email = "admin@artepax.com";
        String rawPassword = "secret123";
        String bcryptHash = "$2a$10$dummyBcryptHashValueForTesting";

        Usuario usuario = buildUsuario(bcryptHash, true, "ADMIN");
        LoginRequest request = buildRequest(email, rawPassword);

        when(usuarioRepository.findByCorreoAndTipoAuth(eq(email), eq("LOCAL")))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(eq(rawPassword), eq(bcryptHash)))
                .thenReturn(true);
        when(jwtService.generateToken(any(Usuario.class)))
                .thenReturn("jwt.token.here");

        AuthResponse response = localAuthService.login(request);

        assertNotNull(response, "Response must not be null");
        assertEquals("jwt.token.here", response.getToken(), "Token must match generated JWT");
    }

    // -------------------------------------------------------------------------
    // 2. Wrong password
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Wrong password returns 401 with generic message")
    void login_wrongPassword() {
        String email = "admin@artepax.com";
        String rawPassword = "wrongpass";
        String bcryptHash = "$2a$10$dummyBcryptHashValueForTesting";

        Usuario usuario = buildUsuario(bcryptHash, true, "ADMIN");
        LoginRequest request = buildRequest(email, rawPassword);

        when(usuarioRepository.findByCorreoAndTipoAuth(eq(email), eq("LOCAL")))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(eq(rawPassword), eq(bcryptHash)))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> localAuthService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode(),
                "Wrong password must return 401");
        assertEquals("Credenciales inválidas", exception.getReason(),
                "Error message must be generic (no user enumeration)");
    }

    // -------------------------------------------------------------------------
    // 3. Unknown email
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unknown email returns 401 with generic message")
    void login_unknownEmail() {
        String email = "nonexistent@artepax.com";
        LoginRequest request = buildRequest(email, "anypassword");

        when(usuarioRepository.findByCorreoAndTipoAuth(eq(email), eq("LOCAL")))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> localAuthService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode(),
                "Unknown email must return 401");
        assertEquals("Credenciales inválidas", exception.getReason(),
                "Error message must be the same as wrong password (no user enumeration)");
    }

    // -------------------------------------------------------------------------
    // 4. GOOGLE user attempts local login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GOOGLE user gets 401 — query-level filter prevents match")
    void login_googleUser() {
        String email = "googleuser@gmail.com";
        LoginRequest request = buildRequest(email, "anypassword");

        // The query itself filters: findByCorreoAndTipoAuth(email, "LOCAL")
        // returns empty for a GOOGLE user. Do NOT mock finding the user first
        // and then checking tipoAuth — the repo is the filter.
        when(usuarioRepository.findByCorreoAndTipoAuth(eq(email), eq("LOCAL")))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> localAuthService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode(),
                "GOOGLE user attempting local login must receive 401");
        assertEquals("Credenciales inválidas", exception.getReason(),
                "Error message must be the same generic message");
    }

    // -------------------------------------------------------------------------
    // 5. Inactive user
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Inactive user returns 403 Forbidden")
    void login_inactiveUser() {
        String email = "inactive@artepax.com";
        String bcryptHash = "$2a$10$dummyBcryptHashValueForTesting";

        Usuario usuario = buildUsuario(bcryptHash, false, "ADMIN");
        LoginRequest request = buildRequest(email, "secret123");

        when(usuarioRepository.findByCorreoAndTipoAuth(eq(email), eq("LOCAL")))
                .thenReturn(Optional.of(usuario));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> localAuthService.login(request)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode(),
                "Inactive user must receive 403");
        assertEquals("Usuario inactivo", exception.getReason(),
                "Message must indicate user is inactive");
    }

    // -------------------------------------------------------------------------
    // 6. Non-ADMIN role
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Non-ADMIN role returns 403 Forbidden")
    void login_nonAdminRole() {
        String email = "user@artepax.com";
        String bcryptHash = "$2a$10$dummyBcryptHashValueForTesting";

        Usuario usuario = buildUsuario(bcryptHash, true, "USER");
        LoginRequest request = buildRequest(email, "secret123");

        when(usuarioRepository.findByCorreoAndTipoAuth(eq(email), eq("LOCAL")))
                .thenReturn(Optional.of(usuario));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> localAuthService.login(request)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode(),
                "Non-ADMIN role must receive 403");
        assertEquals("No tiene permisos", exception.getReason(),
                "Message must indicate insufficient permissions");
    }

    // -------------------------------------------------------------------------
    // 7. Non-BCrypt password hash (e.g., "GOOGLE_AUTH" placeholder)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Non-BCrypt hash in password field returns 401 gracefully")
    void login_nonBCryptHash() {
        String email = "legacy@artepax.com";
        String nonBcryptPassword = "GOOGLE_AUTH";

        Usuario usuario = buildUsuario(nonBcryptPassword, true, "ADMIN");
        LoginRequest request = buildRequest(email, "somepassword");

        when(usuarioRepository.findByCorreoAndTipoAuth(eq(email), eq("LOCAL")))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), eq(nonBcryptPassword)))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> localAuthService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode(),
                "Non-BCrypt hash must return 401, never 500");
        assertEquals("Credenciales inválidas", exception.getReason(),
                "Error message must be generic — no internal detail exposed");
    }
}
