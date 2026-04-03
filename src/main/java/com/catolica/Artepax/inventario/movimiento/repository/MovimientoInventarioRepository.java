package com.catolica.Artepax.inventario.movimiento.repository;

import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.producto.entity.Producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    // 🔍 Obtener todos los movimientos de un producto
    List<MovimientoInventario> findByProducto(Producto producto);

}