package com.catolica.Artepax.authentication.user.repository;

import com.catolica.Artepax.authentication.user.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // 🔍 Buscar por correo (LOGIN)
    Optional<Usuario> findByCorreo(String correo);

    // 🔍 Verificar si existe
    boolean existsByCorreo(String correo);

    // 🔍 Verificar si existe (case-insensitive, para registro de nuevo usuario)
    boolean existsByCorreoIgnoreCase(String correo);

    // 🔢 Contar usuarios activos por nombre de rol (para validar último ADMIN)
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

    @Query("SELECT u FROM Usuario u JOIN u.rol r " +
            "WHERE r.nombre = 'ADMIN' AND u.estado = true")
    List<Usuario> findAdminsActivos();
}