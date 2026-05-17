package com.catolica.Artepax.clientes.repository;

import com.catolica.Artepax.clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    // Solo activos — ordenados por nombre
    List<Cliente> findByActivoTrueOrderByNombreAsc();

    // Búsqueda parcial case-insensitive solo en activos
    List<Cliente> findByNombreContainingIgnoreCaseAndActivoTrueOrderByNombreAsc(String nombre);

    // Para admin — ver inactivos
    List<Cliente> findByActivoFalseOrderByNombreAsc();
}
