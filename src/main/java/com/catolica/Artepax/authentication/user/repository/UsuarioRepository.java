package com.catolica.Artepax.authentication.user.repository;

import com.catolica.Artepax.authentication.user.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Usuario} entities.
 * <p>
 * Provides lookups by email, email + auth type, and admin-specific queries
 * used by authentication and user management flows.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Finds a user by their email address.
     *
     * @param correo the email address (exact match)
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Checks whether a user with the given email already exists (exact match).
     *
     * @param correo the email address
     * @return {@code true} if a user with that email exists
     */
    boolean existsByCorreo(String correo);

    /**
     * Checks whether a user with the given email already exists (case-insensitive).
     * <p>
     * Used during registration to prevent duplicate emails regardless of case.
     *
     * @param correo the email address
     * @return {@code true} if a user with that email exists (case-insensitive)
     */
    boolean existsByCorreoIgnoreCase(String correo);

    /**
     * Counts active users by role name.
     * <p>
     * Used by {@link com.catolica.Artepax.authentication.user.service.UsuarioService#cambiarEstado}
     * to guard against deactivating the last active ADMIN.
     *
     * @param rolNombre the role name (e.g. {@code "ADMIN"})
     * @return the number of active users with that role
     */
    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.rol r WHERE r.nombre = :rolNombre AND u.estado = true")
    long countActivosByRolNombre(@Param("rolNombre") String rolNombre);

    /**
     * Finds a user by email and auth type.
     * <p>
     * This query filters at the database level — users with {@code tipoAuth = 'GOOGLE'}
     * never match when {@code tipoAuth} is {@code "LOCAL"}, and vice versa.
     *
     * @param correo   the user's email
     * @param tipoAuth the authentication type ({@code "LOCAL"} or {@code "GOOGLE"})
     * @return the user if found, empty otherwise
     */
    Optional<Usuario> findByCorreoAndTipoAuth(String correo, String tipoAuth);

    /**
     * Finds all active ADMIN users.
     *
     * @return a list of active ADMIN {@link Usuario} entities
     */
    @Query("SELECT u FROM Usuario u JOIN u.rol r " +
            "WHERE r.nombre = 'ADMIN' AND u.estado = true")
    List<Usuario> findAdminsActivos();
}