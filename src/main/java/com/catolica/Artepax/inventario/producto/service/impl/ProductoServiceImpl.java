package com.catolica.Artepax.inventario.producto.service.impl;

import com.catolica.Artepax.inventario.producto.dto.ProductoRequestDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoUpdateDTO;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.mapper.ProductoMapper;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import com.catolica.Artepax.inventario.producto.service.ProductoService;

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

    // ✅ Crear producto
    @Override
    public ProductoResponseDTO crearProducto(ProductoRequestDTO dto) {

        // 🔥 Validar nombre duplicado
        if (productoRepository.existsByNombre(dto.getNombre())) {
            throw new RuntimeException("El producto ya existe");
        }

        Producto producto = productoMapper.toEntity(dto);
        Producto guardado = productoRepository.save(producto);

        return productoMapper.toDTO(guardado);
    }

    // ✅ Obtener todos
    @Override
    public List<ProductoResponseDTO> obtenerTodos() {
        return productoRepository.findAll()
                .stream()
                .map(productoMapper::toDTO)
                .toList();
    }

    // ✅ Obtener por ID
    @Override
    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        return productoMapper.toDTO(producto);
    }

    // ✅ Actualizar
    @Override
    public ProductoResponseDTO actualizar(Long id, ProductoUpdateDTO dto) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // 🔥 actualizar con mapper (limpio)
        productoMapper.updateEntity(producto, dto);

        Producto actualizado = productoRepository.save(producto);

        return productoMapper.toDTO(actualizado);
    }

    // ✅ Eliminar
    @Override
    public void eliminar(Long id) {

        if (!productoRepository.existsById(id)) {
            throw new RuntimeException("Producto no existe");
        }

        productoRepository.deleteById(id);
    }
}