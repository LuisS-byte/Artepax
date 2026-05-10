package com.catolica.Artepax.inventario.producto.repository;

import com.catolica.Artepax.inventario.producto.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // 🔍 Buscar por nombre (opcional)
    Optional<Producto> findByNombre(String nombre);

    // 🔍 Validar si ya existe un producto con ese nombre
    boolean existsByNombre(String nombre);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Producto p
            WHERE p.id IN :ids
            """)
    List<Producto> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);
}
