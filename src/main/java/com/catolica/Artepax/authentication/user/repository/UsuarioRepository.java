package com.catolica.Artepax.authentication.user.repository;

import com.catolica.Artepax.authentication.user.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // 🔍 Buscar por correo (LOGIN)
    Optional<Usuario> findByCorreo(String correo);

    // 🔍 Verificar si existe
    boolean existsByCorreo(String correo);
}