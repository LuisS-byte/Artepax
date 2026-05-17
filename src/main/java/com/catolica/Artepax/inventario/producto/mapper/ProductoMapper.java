package com.catolica.Artepax.inventario.producto.mapper;

import com.catolica.Artepax.inventario.producto.dto.ProductoRequestDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoUpdateDTO;
import com.catolica.Artepax.inventario.producto.entity.Producto;

import org.springframework.stereotype.Component;

/**
 * Mapper between {@link Producto} entity and its DTOs.
 * Handles conversion from {@link ProductoRequestDTO} to entity, entity to
 * {@link ProductoResponseDTO}, and partial updates via {@link ProductoUpdateDTO}.
 */
@Component
public class ProductoMapper {

    /**
     * Converts a creation request DTO into a new {@link Producto} entity.
     *
     * @param dto the request data with product fields
     * @return a new {@code Producto} with fields mapped from the DTO
     */
    public Producto toEntity(ProductoRequestDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setCantidadTotal(dto.getCantidadTotal());
        producto.setEstado(dto.getEstado());
        return producto;
    }

    /**
     * Converts a {@link Producto} entity into a response DTO.
     *
     * @param producto the product entity to convert
     * @return a {@code ProductoResponseDTO} with all product fields populated
     */
    public ProductoResponseDTO toDTO(Producto producto) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setCantidadTotal(producto.getCantidadTotal());
        dto.setPrecioUnitario(producto.getPrecioUnitario());
        dto.setStockMinimo(producto.getStockMinimo());
        dto.setEstado(producto.getEstado());
        dto.setFechaCreacion(producto.getFechaCreacion());
        return dto;
    }

    /**
     * Applies non-null fields from an update DTO to an existing entity.
     * Fields that are {@code null} in the DTO are left unchanged.
     *
     * @param producto the existing entity to update
     * @param dto      the DTO carrying the new values
     */
    public void updateEntity(Producto producto, ProductoUpdateDTO dto) {
        if (dto.getNombre() != null) producto.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) producto.setDescripcion(dto.getDescripcion());
        if (dto.getEstado() != null) producto.setEstado(dto.getEstado());
        if (dto.getPrecioUnitario() != null) producto.setPrecioUnitario(dto.getPrecioUnitario());
        if (dto.getStockMinimo() != null) producto.setStockMinimo(dto.getStockMinimo());
    }
}
