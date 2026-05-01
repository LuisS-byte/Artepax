package com.catolica.Artepax.inventario.producto.repository;

import com.catolica.Artepax.inventario.producto.entity.StockProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface StockProductoRepository extends JpaRepository<StockProducto, Long> {

    @Query("SELECT sp FROM StockProducto sp WHERE sp.idProducto IN :ids")
    List<StockProducto> findAllByIdProductoIn(@Param("ids") List<Long> ids);
}