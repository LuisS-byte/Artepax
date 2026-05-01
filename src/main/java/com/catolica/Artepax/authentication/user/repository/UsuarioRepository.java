package com.catolica.Artepax.authentication.user.repository;

import com.catolica.Artepax.authentication.user.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // 🔍 Buscar por correo (LOGIN)
    Optional<Usuario> findByCorreo(String correo);

    // 🔍 Verificar si existe
    boolean existsByCorreo(String correo);

    @Query("SELECT u FROM Usuario u JOIN u.rol r " +
            "WHERE r.nombre = 'ADMIN' AND u.estado = true")
    List<Usuario> findAdminsActivos();
}