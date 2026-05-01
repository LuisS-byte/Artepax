package com.catolica.Artepax.inventario.movimiento.service;

import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.StockProductoResponseDTO;

import java.util.List;

public interface MovimientoService {

    void registrarEntrada(MovimientoRequestDTO dto);
    void registrarSalida(MovimientoRequestDTO dto);
    void registrarAjuste(Long productoId, Integer cantidadFisica, String motivo);
    StockProductoResponseDTO obtenerStock(Long productoId);
    List<MovimientoResponseDTO> obtenerMovimientosPorProducto(Long productoId);
}
