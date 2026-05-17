package com.catolica.Artepax.inventario.decoracion.repository;


import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProductoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for the {@link DecoracionProducto} junction entity.
 * Provides queries to retrieve product associations for decoration packages,
 * including eager-loading of the related {@link com.catolica.Artepax.inventario.producto.entity.Producto}.
 */
public interface DecoracionProductoRepository  extends JpaRepository<DecoracionProducto, DecoracionProductoId> {

    /**
     * Finds all product associations for a given decoration.
     *
     * @param decoracionId the decoration ID
     * @return the list of product associations
     */
    List<DecoracionProducto> findByDecoracionId(Long decoracionId);

    /**
     * Finds all product associations for the given decoration IDs,
     * eagerly loading the related product.
     *
     * @param ids the list of decoration IDs
     * @return the list of product associations with products loaded
     */
    @Query("SELECT dp FROM DecoracionProducto dp JOIN FETCH dp.producto WHERE dp.decoracion.id IN :ids")
    List<DecoracionProducto> findByDecoracionIds(@Param("ids") List<Long> ids);

    /**
     * Finds all product associations for a given decoration,
     * eagerly loading the related product.
     *
     * @param decoracionId the decoration ID
     * @return the list of product associations with products loaded
     */
    @Query("""
        SELECT dp
        FROM DecoracionProducto dp
        JOIN FETCH dp.producto
        WHERE dp.decoracion.id = :decoracionId
    """)
    List<DecoracionProducto> findByDecoracionIdWithProducto(@Param("decoracionId") Long decoracionId);
}