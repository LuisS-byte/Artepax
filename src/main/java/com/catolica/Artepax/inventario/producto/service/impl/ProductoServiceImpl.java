package com.catolica.Artepax.inventario.producto.service.impl;

import com.catolica.Artepax.inventario.producto.exception.ProductoDuplicadoException;
import com.catolica.Artepax.inventario.producto.dto.ProductoRequestDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoUpdateDTO;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.exception.ProductoNotFoundException;
import com.catolica.Artepax.inventario.producto.mapper.ProductoMapper;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import com.catolica.Artepax.inventario.producto.service.ProductoService;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               ProductoMapper productoMapper) {
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
    }

    @Override
    @Transactional
    public ProductoResponseDTO crearProducto(ProductoRequestDTO dto) {

        if (productoRepository.existsByNombre(dto.getNombre())) {
            throw new ProductoDuplicadoException(dto.getNombre());
        }

        Producto producto = productoMapper.toEntity(dto);
        Producto guardado = productoRepository.save(producto);
        return productoMapper.toDTO(guardado);
    }

    @Override
    public List<ProductoResponseDTO> obtenerTodos() {
        return productoRepository.findAll()
                .stream()
                .map(productoMapper::toDTO)
                .toList();
    }

    @Override
    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        return productoMapper.toDTO(producto);
    }

    @Override
    @Transactional
    public ProductoResponseDTO actualizar(Long id, ProductoUpdateDTO dto) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));

        productoMapper.updateEntity(producto, dto);

        Producto actualizado = productoRepository.save(producto);
        return productoMapper.toDTO(actualizado);
    }

    // Reemplazar eliminar():
    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        producto.setEstado("INACTIVO");
        productoRepository.save(producto);
    }

    // Agregar reactivar():
    @Override
    @Transactional
    public ProductoResponseDTO reactivar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        producto.setEstado("ACTIVO");
        return productoMapper.toDTO(productoRepository.save(producto));
    }

    @Override
    public List<ProductoResponseDTO> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre)
                .stream()
                .map(productoMapper::toDTO)
                .toList();
    }
}