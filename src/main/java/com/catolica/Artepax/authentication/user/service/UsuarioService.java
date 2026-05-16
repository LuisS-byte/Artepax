package com.catolica.Artepax.authentication.user.service;

import com.catolica.Artepax.authentication.user.dto.UsuarioCreateRequest;
import com.catolica.Artepax.authentication.user.dto.UsuarioEstadoRequest;
import com.catolica.Artepax.authentication.user.dto.UsuarioResponseDTO;
import com.catolica.Artepax.authentication.user.entity.Rol;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.RolRepository;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // 🔐 ESTE METODO LO USA SPRING SECURITY (OBLIGATORIO)
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    // 🔍 Buscar usuario (para Google login)
    public Usuario findByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElse(null);
    }

    /**
     * Creates a new local user with BCrypt-encoded password.
     * <p>
     * Validates that the email is not already in use (case-insensitive),
     * that the role exists, then persists the user with {@code tipoAuth = "LOCAL"}
     * and {@code estado = true}.
     *
     * @param request the creation request with user details
     * @return a DTO representing the newly created user (password excluded)
     * @throws ResponseStatusException 400 if email is duplicate or role is invalid
     */
    @Transactional
    public UsuarioResponseDTO crearUsuarioLocal(UsuarioCreateRequest request) {
        if (usuarioRepository.existsByCorreoIgnoreCase(request.correo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ya existe un usuario con ese correo");
        }

        Rol rol = rolRepository.findById(request.rolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Rol no válido"));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .correo(request.correo().trim().toLowerCase())
                .telefono(request.telefono())
                .password(passwordEncoder.encode(request.password()))
                .tipoAuth("LOCAL")
                .estado(true)
                .rol(rol)
                .build();

        Usuario saved = usuarioRepository.save(usuario);
        return toResponseDTO(saved);
    }

    /**
     * Lists all registered users.
     * <p>
     * Returns DTOs that intentionally omit the password field.
     *
     * @return a list of user DTOs
     */
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Toggles the active state of a user.
     * <p>
     * Guards against self-deactivation and deactivating the last active ADMIN.
     *
     * @param id                the target user's ID
     * @param request           the state change request
     * @param usuarioAutenticado the currently authenticated user
     * @return a DTO representing the updated user
     * @throws ResponseStatusException 404 if user not found,
     *                                  400 if self-deactivation or last active ADMIN
     */
    @Transactional
    public UsuarioResponseDTO cambiarEstado(Long id, UsuarioEstadoRequest request,
                                            Usuario usuarioAutenticado) {
        Usuario target = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"));

        // Self-deactivation guard: admin cannot deactivate themselves
        if (!request.activo() && target.getIdUsuario().equals(usuarioAutenticado.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puede desactivarse a sí mismo");
        }

        // Last active ADMIN guard: only applies when deactivating an active ADMIN
        if (!request.activo()
                && target.getEstado()
                && target.getRol() != null
                && "ADMIN".equals(target.getRol().getNombre())
                && usuarioRepository.countActivosByRolNombre("ADMIN") == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puede desactivar al último ADMIN activo");
        }

        target.setEstado(request.activo());
        Usuario saved = usuarioRepository.save(target);
        return toResponseDTO(saved);
    }

    /**
     * Maps a {@link Usuario} entity to a {@link UsuarioResponseDTO},
     * intentionally excluding the password field.
     *
     * @param u the user entity
     * @return a DTO with public user data
     */
    private UsuarioResponseDTO toResponseDTO(Usuario u) {
        return new UsuarioResponseDTO(
                u.getIdUsuario(),
                u.getNombre(),
                u.getCorreo(),
                u.getTelefono(),
                u.getTipoAuth(),
                u.getEstado(),
                u.getRol() != null ? u.getRol().getIdRol() : null,
                u.getRol() != null ? u.getRol().getNombre() : null,
                u.getFechaCreacion()
        );
    }
}