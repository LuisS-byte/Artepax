package com.catolica.Artepax.inventario.movimiento.repository;

import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.producto.entity.Producto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    // 🔍 Obtener todos los movimientos de un producto
    List<MovimientoInventario> findByProducto(Producto producto);

    List<MovimientoInventario> findByProductoId(Long productoId);

    // Listado global paginado — evento es nullable (LEFT JOIN)
    @Query(value = "SELECT m FROM MovimientoInventario m JOIN FETCH m.producto LEFT JOIN FETCH m.evento e LEFT JOIN FETCH e.cliente LEFT JOIN FETCH e.estadoEvento",
           countQuery = "SELECT COUNT(m) FROM MovimientoInventario m")
    Page<MovimientoInventario> findAllWithProductoYEvento(Pageable pageable);

    @Query(value = """
        SELECT m
        FROM MovimientoInventario m
        JOIN FETCH m.producto
        LEFT JOIN FETCH m.evento e
        LEFT JOIN FETCH e.cliente
        WHERE m.producto.id = :productoId
        ORDER BY m.fecha DESC
    """,
           countQuery = "SELECT COUNT(m) FROM MovimientoInventario m WHERE m.producto.id = :productoId")
    Page<MovimientoInventario> findByProductoIdWithRelationsOrderByFechaDesc(@Param("productoId") Long productoId, Pageable pageable);
}