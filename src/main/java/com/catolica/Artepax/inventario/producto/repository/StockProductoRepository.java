package com.catolica.Artepax.inventario.producto.repository;

import com.catolica.Artepax.inventario.producto.entity.StockProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
/**
 * Repository for the read-only {@link StockProducto} view entity.
 * Maps to the database view {@code vista_stock_producto} which computes
 * available stock as total minus reserved quantity.
 */
@Repository
public interface StockProductoRepository extends JpaRepository<StockProducto, Long> {

    /**
     * Finds stock information for all products whose IDs are in the given list.
     *
     * @param ids the list of product IDs to look up
     * @return the list of matching stock views
     */
    @Query("SELECT sp FROM StockProducto sp WHERE sp.idProducto IN :ids")
    List<StockProducto> findAllByIdProductoIn(@Param("ids") List<Long> ids);
}