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
import java.util.Set;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_INACTIVO = "INACTIVO";
    private static final Set<String> ESTADOS_VALIDOS = Set.of(ESTADO_ACTIVO, ESTADO_INACTIVO);

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               ProductoMapper productoMapper) {
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
    }

    private String normalizarNombre(String nombre) {
        return nombre == null ? null : nombre.trim().replaceAll("\\s+", " ");
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) return ESTADO_ACTIVO;
        String normalizado = estado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de producto inválido: " + estado + ". Válidos: " + ESTADOS_VALIDOS);
        }
        return normalizado;
    }

    private void validarNombreDisponible(String nombre, Long idActual) {
        productoRepository.findByNombreIgnoreCase(nombre)
                .filter(p -> idActual == null || !p.getId().equals(idActual))
                .ifPresent(p -> {
                    throw new ProductoDuplicadoException(nombre);
                });
    }

    @Override
    @Transactional
    public ProductoResponseDTO crearProducto(ProductoRequestDTO dto) {
        String nombreNormalizado = normalizarNombre(dto.getNombre());
        validarNombreDisponible(nombreNormalizado, null);
        dto.setNombre(nombreNormalizado);

        Producto producto = productoMapper.toEntity(dto);
        producto.setEstado(normalizarEstado(producto.getEstado()));
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

        if (dto.getNombre() != null) {
            String nombreNormalizado = normalizarNombre(dto.getNombre());
            validarNombreDisponible(nombreNormalizado, id);
            dto.setNombre(nombreNormalizado);
        }

        productoMapper.updateEntity(producto, dto);

        if (dto.getEstado() != null) {
            producto.setEstado(normalizarEstado(dto.getEstado()));
        }

        Producto actualizado = productoRepository.save(producto);
        return productoMapper.toDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        producto.setEstado("INACTIVO");
        productoRepository.save(producto);
    }

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