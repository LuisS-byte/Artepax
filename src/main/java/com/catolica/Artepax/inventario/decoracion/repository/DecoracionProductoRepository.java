package com.catolica.Artepax.inventario.decoracion.repository;


import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProductoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecoracionProductoRepository  extends JpaRepository<DecoracionProducto, DecoracionProductoId> {

    // traer todos los productos requeridos de una decoracion
    List<DecoracionProducto> findByDecoracionId(Long decoracionId);
}