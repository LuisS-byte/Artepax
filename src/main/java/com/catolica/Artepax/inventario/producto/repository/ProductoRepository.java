package com.catolica.Artepax.inventario.producto.repository;

import com.catolica.Artepax.inventario.producto.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // 🔍 Buscar por nombre (opcional)
    Optional<Producto> findByNombre(String nombre);

    // 🔍 Validar si ya existe un producto con ese nombre
    boolean existsByNombre(String nombre);
}