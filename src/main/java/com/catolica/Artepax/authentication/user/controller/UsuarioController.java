package com.catolica.Artepax.authentication.user.controller;

import com.catolica.Artepax.authentication.user.dto.UsuarioCreateRequest;
import com.catolica.Artepax.authentication.user.dto.UsuarioEstadoRequest;
import com.catolica.Artepax.authentication.user.dto.UsuarioResponseDTO;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for user management.
 * <p>
 * Admin endpoints ({@code POST /api/usuarios}, {@code GET /api/usuarios},
 * {@code PATCH /api/usuarios/{id}/estado}) require {@code ROLE_ADMIN}.
 * {@code GET /api/usuarios/me} is accessible to any authenticated user.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Returns the currently authenticated user's profile information.
     * <p>
     * This endpoint is intentionally kept outside the ADMIN-only filter so that
     * any authenticated user can view their own profile.
     *
     * @param auth the current authentication
     * @return 200 with user profile fields, or 401 if not authenticated
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> obtenerUsuarioActual(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = (Usuario) auth.getPrincipal();

        Map<String, Object> body = Map.of(
                "id", usuario.getIdUsuario(),
                "nombre", usuario.getNombre(),
                "correo", usuario.getCorreo(),
                "telefono", usuario.getTelefono() != null ? usuario.getTelefono() : "",
                "rol", usuario.getRol() != null ? usuario.getRol().getNombre() : ""
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Creates a new local user (ADMIN only).
     *
     * @param request the creation payload
     * @return 201 with the created user DTO
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> crearUsuario(
            @Valid @RequestBody UsuarioCreateRequest request) {
        UsuarioResponseDTO response = usuarioService.crearUsuarioLocal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all registered users (ADMIN only).
     *
     * @return 200 with a list of user DTOs
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    /**
     * Toggles the active state of a user (ADMIN only).
     * <p>
     * Guards against self-deactivation and deactivating the last active ADMIN
     * (enforced by the service layer).
     *
     * @param id                 the target user's ID
     * @param request            the state change payload
     * @param usuarioAutenticado the currently authenticated admin
     * @return 200 with the updated user DTO
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioEstadoRequest request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        return ResponseEntity.ok(
                usuarioService.cambiarEstado(id, request, usuarioAutenticado));
    }
}
