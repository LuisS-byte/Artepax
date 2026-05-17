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

/**
 * Implementation of {@link ProductoService} for product CRUD and inventory management.
 * <p>
 * Handles name normalization, duplicate name validation, state validation (ACTIVO/INACTIVO),
 * and soft-delete via state transition. Note that this service uses
 * {@link jakarta.transaction.Transactional @Transactional} (Jakarta) instead of the
 * Spring variant — this is a known inconsistency in the codebase.
 * </p>
 */
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

    /**
     * Normalizes a product name by trimming whitespace and collapsing multiple spaces.
     *
     * @param nombre the raw name to normalize
     * @return the normalized name, or {@code null} if input was null
     */
    private String normalizarNombre(String nombre) {
        return nombre == null ? null : nombre.trim().replaceAll("\\s+", " ");
    }

    /**
     * Normalizes and validates an estado (state) value.
     * Defaults to {@code ACTIVO} if the input is null or blank.
     *
     * @param estado the raw state value
     * @return the normalized uppercase state
     * @throws IllegalArgumentException if the value is not ACTIVO or INACTIVO
     */
    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) return ESTADO_ACTIVO;
        String normalizado = estado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de producto inválido: " + estado + ". Válidos: " + ESTADOS_VALIDOS);
        }
        return normalizado;
    }

    /**
     * Validates that no other product already uses the given name (case-insensitive).
     * When {@code idActual} is provided, the check excludes the product being updated.
     *
     * @param nombre   the name to check for uniqueness
     * @param idActual the ID of the product being updated, or {@code null} for creation
     * @throws ProductoDuplicadoException if a duplicate name is found
     */
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
        // Normalizes the name, validates uniqueness, maps the DTO to an entity,
        // normalizes the state, saves, and returns the created product.
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
        // Retrieves all products from the repository and maps them to DTOs.
        return productoRepository.findAll()
                .stream()
                .map(productoMapper::toDTO)
                .toList();
    }

    @Override
    public ProductoResponseDTO obtenerPorId(Long id) {
        // Finds the product by ID or throws ProductoNotFoundException.
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        return productoMapper.toDTO(producto);
    }

    @Override
    @Transactional
    public ProductoResponseDTO actualizar(Long id, ProductoUpdateDTO dto) {
        // Finds the existing product, validates name uniqueness if changed,
        // applies partial updates via the mapper, and saves.

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
        // Soft-deletes the product by setting its state to INACTIVO.
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        producto.setEstado("INACTIVO");
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public ProductoResponseDTO reactivar(Long id) {
        // Reactivates the product by setting its state back to ACTIVO.
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
        producto.setEstado("ACTIVO");
        return productoMapper.toDTO(productoRepository.save(producto));
    }

    @Override
    public List<ProductoResponseDTO> buscarPorNombre(String nombre) {
        // Searches products by name (case-insensitive, partial match) and maps to DTOs.
        return productoRepository.findByNombreContainingIgnoreCase(nombre)
                .stream()
                .map(productoMapper::toDTO)
                .toList();
    }
}