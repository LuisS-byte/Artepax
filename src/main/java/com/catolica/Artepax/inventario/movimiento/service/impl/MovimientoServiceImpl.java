package com.catolica.Artepax.inventario.movimiento.service.impl;

import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.movimiento.mapper.MovimientoMapper;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;
import com.catolica.Artepax.inventario.movimiento.service.MovimientoService;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.exception.ProductoNotFoundException;
import com.catolica.Artepax.inventario.producto.exception.StockInsuficienteException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovimientoServiceImpl implements MovimientoService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoMapper movimientoMapper;

    public MovimientoServiceImpl(MovimientoInventarioRepository movimientoRepository,
                                 ProductoRepository productoRepository,
                                 MovimientoMapper movimientoMapper) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.movimientoMapper = movimientoMapper;
    }
    @Override
    @Transactional
    public void registrarEntrada(MovimientoRequestDTO dto) {

        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new ProductoNotFoundException(dto.getProductoId()));

        producto.setCantidadTotal(producto.getCantidadTotal() + dto.getCantidad());
        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento("ENTRADA");
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setMotivo(dto.getMotivo());
        movimientoRepository.save(movimiento);
    }

    @Override
    @Transactional
    public void registrarSalida(MovimientoRequestDTO dto) {

        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new ProductoNotFoundException(dto.getProductoId()));

        if (producto.getCantidadTotal() < dto.getCantidad()) {
            throw new StockInsuficienteException(
                    producto.getNombre(),
                    producto.getCantidadTotal(),
                    dto.getCantidad()
            );
        }

        producto.setCantidadTotal(producto.getCantidadTotal() - dto.getCantidad());
        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento("SALIDA");
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setMotivo(dto.getMotivo());
        movimientoRepository.save(movimiento);
    }

    @Override
    public Integer obtenerStock(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ProductoNotFoundException(productoId));
        return producto.getCantidadTotal();
    }
    // nuevo método
    @Override
    public List<MovimientoResponseDTO> obtenerMovimientosPorProducto(Long productoId) {
        if (!productoRepository.existsById(productoId)) {
            throw new ProductoNotFoundException(productoId);
        }
        return movimientoRepository.findByProductoId(productoId)
                .stream()
                .map(movimientoMapper::toDTO)
                .toList();
    }
}