package com.catolica.Artepax.inventario.decoracion.repository;


import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProductoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DecoracionProductoRepository  extends JpaRepository<DecoracionProducto, DecoracionProductoId> {

    // traer todos los productos requeridos de una decoracion
    List<DecoracionProducto> findByDecoracionId(Long decoracionId);

    @Query("SELECT dp FROM DecoracionProducto dp JOIN FETCH dp.producto WHERE dp.decoracion.id IN :ids")
    List<DecoracionProducto> findByDecoracionIds(@Param("ids") List<Long> ids);

    @Query("""
        SELECT dp
        FROM DecoracionProducto dp
        JOIN FETCH dp.producto
        WHERE dp.decoracion.id = :decoracionId
    """)
    List<DecoracionProducto> findByDecoracionIdWithProducto(@Param("decoracionId") Long decoracionId);
}