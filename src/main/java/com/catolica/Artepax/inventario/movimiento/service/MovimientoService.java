package com.catolica.Artepax.inventario.movimiento.service;

import com.catolica.Artepax.inventario.movimiento.dto.AjusteInventarioRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.StockProductoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MovimientoService {

    void registrarEntrada(MovimientoRequestDTO dto);
    void registrarSalida(MovimientoRequestDTO dto);
    void registrarAjuste(Long productoId, AjusteInventarioRequestDTO request);
    StockProductoResponseDTO obtenerStock(Long productoId);
    Page<MovimientoResponseDTO> obtenerMovimientosPorProducto(Long productoId, Pageable pageable);

    Page<MovimientoResponseDTO> obtenerTodosMovimientos(Pageable pageable);
}
