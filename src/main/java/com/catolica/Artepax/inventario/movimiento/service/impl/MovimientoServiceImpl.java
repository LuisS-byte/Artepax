package com.catolica.Artepax.inventario.movimiento.service.impl;

import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;
import com.catolica.Artepax.inventario.movimiento.service.MovimientoService;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;

import org.springframework.stereotype.Service;

@Service
public class MovimientoServiceImpl implements MovimientoService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;

    public MovimientoServiceImpl(MovimientoInventarioRepository movimientoRepository,
                                 ProductoRepository productoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
    }

    // 📥 ENTRADA (SUMA STOCK)
    @Override
    public void registrarEntrada(MovimientoRequestDTO dto) {

        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 🔥 sumar stock
        producto.setCantidadTotal(
                producto.getCantidadTotal() + dto.getCantidad()
        );

        productoRepository.save(producto);

        // 🔥 registrar movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento("ENTRADA");
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setMotivo(dto.getMotivo());

        movimientoRepository.save(movimiento);
    }

    // 📤 SALIDA (RESTA STOCK)
    @Override
    public void registrarSalida(MovimientoRequestDTO dto) {

        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 🔥 VALIDAR STOCK
        if (producto.getCantidadTotal() < dto.getCantidad()) {
            throw new RuntimeException("Stock insuficiente");
        }

        // 🔥 restar stock
        producto.setCantidadTotal(
                producto.getCantidadTotal() - dto.getCantidad()
        );

        productoRepository.save(producto);

        // 🔥 registrar movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento("SALIDA");
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setMotivo(dto.getMotivo());

        movimientoRepository.save(movimiento);
    }

    // 📊 OBTENER STOCK
    @Override
    public Integer obtenerStock(Long productoId) {

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        return producto.getCantidadTotal();
    }
}