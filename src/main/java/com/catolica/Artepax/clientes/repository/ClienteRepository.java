package com.catolica.Artepax.clientes.repository;

import com.catolica.Artepax.clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // 🔍 Buscar por correo (para validación)
    Optional<Cliente> findByCorreo(String correo);

    // 🔍 Verificar si ya existe un correo
    boolean existsByCorreo(String correo);

    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}