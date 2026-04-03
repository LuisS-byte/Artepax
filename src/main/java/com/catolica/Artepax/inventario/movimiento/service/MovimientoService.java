package com.catolica.Artepax.inventario.movimiento.service;

import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;

public interface MovimientoService {

    // 📥 Registrar entrada de inventario
    void registrarEntrada(MovimientoRequestDTO dto);

    // 📤 Registrar salida de inventario
    void registrarSalida(MovimientoRequestDTO dto);

    // 📊 Obtener stock actual de un producto
    Integer obtenerStock(Long productoId);
}