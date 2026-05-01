package com.catolica.Artepax.clientes.repository;

import com.catolica.Artepax.clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    // Solo activos
    List<Cliente> findByActivoTrue();
    List<Cliente> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    // Para admin — ver inactivos
    List<Cliente> findByActivoFalse();
}