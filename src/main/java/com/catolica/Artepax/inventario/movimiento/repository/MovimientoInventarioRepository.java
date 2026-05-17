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

/**
 * Repository for {@link MovimientoInventario} entities.
 * Provides queries for stock movement history with eager-loading of related entities.
 */
@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    /**
     * Finds all movements for a given product entity.
     *
     * @param producto the product entity to filter by
     * @return the list of movements for that product
     */
    List<MovimientoInventario> findByProducto(Producto producto);

    /**
     * Finds all movements for a given product ID.
     *
     * @param productoId the product ID to filter by
     * @return the list of movements for that product
     */
    List<MovimientoInventario> findByProductoId(Long productoId);

    /**
     * Returns a paginated list of all movements with eagerly-loaded product,
     * event, client, and event state. Uses {@code LEFT JOIN FETCH} because the
     * event association is nullable.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of movements with related data
     */
    @Query(value = "SELECT m FROM MovimientoInventario m JOIN FETCH m.producto LEFT JOIN FETCH m.evento e LEFT JOIN FETCH e.cliente LEFT JOIN FETCH e.estadoEvento",
           countQuery = "SELECT COUNT(m) FROM MovimientoInventario m")
    Page<MovimientoInventario> findAllWithProductoYEvento(Pageable pageable);

    /**
     * Returns a paginated list of movements for a specific product, ordered by
     * date descending, with eagerly-loaded product and event (including client).
     *
     * @param productoId the product ID to filter by
     * @param pageable   pagination parameters
     * @return a page of movements for the specified product
     */
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