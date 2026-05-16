package com.catolica.Artepax.authentication.user.service;

import com.catolica.Artepax.authentication.user.dto.UsuarioCreateRequest;
import com.catolica.Artepax.authentication.user.dto.UsuarioEstadoRequest;
import com.catolica.Artepax.authentication.user.dto.UsuarioResponseDTO;
import com.catolica.Artepax.authentication.user.entity.Rol;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.RolRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UsuarioService}.
 * <p>
 * Covers the three service methods added in PR 2: creating local users,
 * listing users without exposing passwords, and toggling user active state.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService unit tests")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private Rol buildRol(Integer id, String nombre) {
        return Rol.builder()
                .idRol(id)
                .nombre(nombre)
                .descripcion("test role")
                .build();
    }

    private Usuario buildUsuario(Long id, String nombre, String correo, String password,
                                 String tipoAuth, boolean estado, Rol rol) {
        return Usuario.builder()
                .idUsuario(id)
                .nombre(nombre)
                .correo(correo)
                .password(password)
                .telefono("555-0100")
                .tipoAuth(tipoAuth)
                .estado(estado)
                .rol(rol)
                .build();
    }

    private UsuarioCreateRequest buildCreateRequest(String correo, Integer rolId) {
        return new UsuarioCreateRequest("Test User", correo, "plainPassword", "555-0100", rolId);
    }

    // -------------------------------------------------------------------------
    // 1. crearUsuarioLocal — éxito
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Create local user successfully with BCrypt password and estado=true")
    void crearUsuarioLocal_exito() {
        Rol rol = buildRol(1, "ADMIN");
        UsuarioCreateRequest request = buildCreateRequest("test@artepax.com", 1);

        when(usuarioRepository.existsByCorreoIgnoreCase("test@artepax.com")).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$hashedValue");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setIdUsuario(1L);
            return u;
        });

        UsuarioResponseDTO response = usuarioService.crearUsuarioLocal(request);

        assertNotNull(response, "Response must not be null");
        assertEquals("test@artepax.com", response.correo(), "Correo must match request");
        assertEquals("LOCAL", response.tipoAuth(), "tipoAuth must be LOCAL");
        assertTrue(response.estado(), "estado must be true for new users");
        assertEquals(1, response.rolId(), "rolId must match the role assigned");
        assertEquals("ADMIN", response.rol(), "rol name must match");

        // Password must be hashed, never plaintext
        verify(usuarioRepository).save(argThat(saved ->
                saved.getPassword() != null
                        && !saved.getPassword().equals("plainPassword")
                        && saved.getPassword().startsWith("$2a$")
        ));
    }

    // -------------------------------------------------------------------------
    // 2. crearUsuarioLocal — password BCrypt, no plano
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Password stored as BCrypt hash, never plaintext")
    void crearUsuarioLocal_passwordBCryptNoPlano() {
        Rol rol = buildRol(1, "ADMIN");
        UsuarioCreateRequest request = buildCreateRequest("test@artepax.com", 1);

        when(usuarioRepository.existsByCorreoIgnoreCase(anyString())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$hashedValue");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setIdUsuario(1L);
            return u;
        });

        usuarioService.crearUsuarioLocal(request);

        verify(passwordEncoder).encode("plainPassword");
        verify(usuarioRepository).save(argThat(saved ->
                "$2a$10$hashedValue".equals(saved.getPassword())
        ));
    }

    // -------------------------------------------------------------------------
    // 3. crearUsuarioLocal — correo duplicado
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Duplicate email returns 400 BAD_REQUEST")
    void crearUsuarioLocal_correoDuplicado() {
        UsuarioCreateRequest request = buildCreateRequest("duplicate@artepax.com", 1);

        when(usuarioRepository.existsByCorreoIgnoreCase("duplicate@artepax.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.crearUsuarioLocal(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Ya existe un usuario con ese correo", exception.getReason());
        verify(usuarioRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 4. crearUsuarioLocal — rol inexistente
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Non-existent role returns 400 BAD_REQUEST 'Rol no válido'")
    void crearUsuarioLocal_rolInexistente() {
        UsuarioCreateRequest request = buildCreateRequest("test@artepax.com", 999);

        when(usuarioRepository.existsByCorreoIgnoreCase(anyString())).thenReturn(false);
        when(rolRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.crearUsuarioLocal(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Rol no válido", exception.getReason());
        verify(usuarioRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 5. listarUsuarios — no expone password
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("List users returns DTOs without password field")
    void listarUsuarios_noExponePassword() {
        Rol rol = buildRol(1, "ADMIN");
        Usuario usuario = buildUsuario(1L, "Test User", "test@artepax.com",
                "secret", "LOCAL", true, rol);

        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        List<UsuarioResponseDTO> result = usuarioService.listarUsuarios();

        assertNotNull(result, "Result must not be null");
        assertEquals(1, result.size(), "Must return exactly one user");
        UsuarioResponseDTO dto = result.getFirst();
        assertEquals(1L, dto.id(), "id must match");
        assertEquals("test@artepax.com", dto.correo(), "correo must match");
        assertEquals(1, dto.rolId(), "rolId must be populated");
        assertEquals("ADMIN", dto.rol(), "rol name must be populated");
        // Verify no password-related accessor exists
        // The record has no 'password' method — this is a compile-time guarantee.
        // At runtime, assert the record component count excludes password.
        assertDoesNotThrow(() -> {
            var fields = UsuarioResponseDTO.class.getRecordComponents();
            boolean hasPassword = false;
            for (var f : fields) {
                if ("password".equals(f.getName())) hasPassword = true;
            }
            assertFalse(hasPassword, "DTO must not contain a password field");
        });
    }

    // -------------------------------------------------------------------------
    // 6. cambiarEstado — desactivar exitoso
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deactivate a user successfully when authenticated user is different")
    void cambiarEstado_desactivarExitoso() {
        Rol rol = buildRol(1, "ADMIN");
        Usuario target = buildUsuario(2L, "Target User", "target@artepax.com",
                "hash", "LOCAL", true, rol);
        Usuario autenticado = buildUsuario(1L, "Admin Auth", "admin@artepax.com",
                "hash", "LOCAL", true, rol);
        UsuarioEstadoRequest request = new UsuarioEstadoRequest(false);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));
        // There are other active admins, so count > 1
        when(usuarioRepository.countActivosByRolNombre("ADMIN")).thenReturn(2L);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(target);

        UsuarioResponseDTO response = usuarioService.cambiarEstado(2L, request, autenticado);

        assertNotNull(response, "Response must not be null");
        assertFalse(response.estado(), "Target user must be deactivated");
        // Verify the target was modified
        verify(usuarioRepository).save(argThat(saved -> !saved.getEstado()));
    }

    // -------------------------------------------------------------------------
    // 7. cambiarEstado — activar exitoso
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Activate an inactive user successfully")
    void cambiarEstado_activarExitoso() {
        Rol rol = buildRol(1, "ADMIN");
        Usuario target = buildUsuario(2L, "Inactive User", "inactive@artepax.com",
                "hash", "LOCAL", false, rol);
        Usuario autenticado = buildUsuario(1L, "Admin Auth", "admin@artepax.com",
                "hash", "LOCAL", true, rol);
        UsuarioEstadoRequest request = new UsuarioEstadoRequest(true);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(target);

        UsuarioResponseDTO response = usuarioService.cambiarEstado(2L, request, autenticado);

        assertNotNull(response, "Response must not be null");
        assertTrue(response.estado(), "Target user must be activated");
        verify(usuarioRepository).save(argThat(saved -> saved.getEstado()));
    }

    // -------------------------------------------------------------------------
    // 8. cambiarEstado — usuario no encontrado
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Non-existent user returns 404 NOT_FOUND")
    void cambiarEstado_usuarioNoEncontrado() {
        Usuario autenticado = buildUsuario(1L, "Admin Auth", "admin@artepax.com",
                "hash", "LOCAL", true, buildRol(1, "ADMIN"));
        UsuarioEstadoRequest request = new UsuarioEstadoRequest(false);

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.cambiarEstado(99L, request, autenticado)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Usuario no encontrado", exception.getReason());
        verify(usuarioRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 9. cambiarEstado — autodesactivación
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Self-deactivation returns 400 BAD_REQUEST")
    void cambiarEstado_autodesactivacion() {
        Rol rol = buildRol(1, "ADMIN");
        Usuario autenticado = buildUsuario(1L, "Admin Auth", "admin@artepax.com",
                "hash", "LOCAL", true, rol);
        // Target is the same user (same id)
        Usuario target = buildUsuario(1L, "Admin Auth", "admin@artepax.com",
                "hash", "LOCAL", true, rol);
        UsuarioEstadoRequest request = new UsuarioEstadoRequest(false);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(target));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.cambiarEstado(1L, request, autenticado)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("No puede desactivarse a sí mismo", exception.getReason());
        verify(usuarioRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // 10. cambiarEstado — último ADMIN activo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deactivating the last active ADMIN returns 400 BAD_REQUEST")
    void cambiarEstado_ultimoAdminActivo() {
        Rol rolAdmin = buildRol(1, "ADMIN");
        Usuario target = buildUsuario(2L, "Last Admin", "lastadmin@artepax.com",
                "hash", "LOCAL", true, rolAdmin);
        Usuario autenticado = buildUsuario(1L, "Other Admin", "otheradmin@artepax.com",
                "hash", "LOCAL", true, rolAdmin);
        UsuarioEstadoRequest request = new UsuarioEstadoRequest(false);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));
        // countActivosByRolNombre("ADMIN") returns 1 — this is the last one
        when(usuarioRepository.countActivosByRolNombre("ADMIN")).thenReturn(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.cambiarEstado(2L, request, autenticado)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("No puede desactivar al último ADMIN activo", exception.getReason());
        verify(usuarioRepository, never()).save(any());
    }
}
