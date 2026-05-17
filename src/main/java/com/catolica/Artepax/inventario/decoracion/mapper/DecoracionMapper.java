package com.catolica.Artepax.inventario.decoracion.mapper;


import com.catolica.Artepax.inventario.decoracion.dto.DecoracionProductoResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper between {@link Decoracion} entity and its DTOs.
 * Handles conversion from request DTOs to entity, entity to response DTOs,
 * product associations, and partial updates via {@link DecoracionUpdateDTO}.
 */
@Component
public class DecoracionMapper {

    /**
     * Converts a creation request DTO into a new {@link Decoracion} entity.
     *
     * @param dto the request data with decoration fields
     * @return a new {@code Decoracion} with fields mapped from the DTO
     */
    public Decoracion toEntity(DecoracionRequestDTO dto) {
        Decoracion decoracion = new Decoracion();
        decoracion.setNombre(dto.getNombre());
        decoracion.setDescripcion(dto.getDescripcion());
        decoracion.setPrecio(dto.getPrecio());
        decoracion.setEstado(dto.getEstado());
        return decoracion;
    }

    /**
     * Converts a {@link Decoracion} entity and its associated products into a
     * full response DTO including the product list.
     *
     * @param decoracion the decoration entity
     * @param productos  the list of product associations
     * @return a response DTO with decoration data and its products
     */
    public DecoracionResponseDTO toDTO(Decoracion decoracion, List<DecoracionProducto> productos) {
        DecoracionResponseDTO dto = new DecoracionResponseDTO();
        dto.setId(decoracion.getId());
        dto.setNombre(decoracion.getNombre());
        dto.setDescripcion(decoracion.getDescripcion());
        dto.setPrecio(decoracion.getPrecio());
        dto.setImagenUrl(decoracion.getImagenUrl());
        dto.setEstado(decoracion.getEstado());
        dto.setFechaCreacion(decoracion.getFechaCreacion());

        dto.setProductos(
                productos.stream().map(this::toProductoDTO).toList()
        );

        return dto;
    }

    /**
     * Converts a single product association into its response DTO.
     *
     * @param dp the decoration-product association entity
     * @return a response DTO with product ID, name, and required quantity
     */
    public DecoracionProductoResponseDTO toProductoDTO(DecoracionProducto dp) {
        DecoracionProductoResponseDTO dto = new DecoracionProductoResponseDTO();
        dto.setProductoId(dp.getProducto().getId());
        dto.setProductoNombre(dp.getProducto().getNombre());
        dto.setCantidadRequerida(dp.getCantidadRequerida());
        return dto;
    }

    /**
     * Applies non-null fields from an update DTO to an existing decoration entity.
     * Fields that are {@code null} in the DTO are left unchanged.
     *
     * @param decoracion the existing entity to update
     * @param dto        the DTO carrying the new values
     */
    public void updateEntity(Decoracion decoracion, DecoracionUpdateDTO dto) {
        if (dto.getNombre() != null) decoracion.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) decoracion.setDescripcion(dto.getDescripcion());
        if (dto.getPrecio() != null) decoracion.setPrecio(dto.getPrecio());
        if (dto.getEstado() != null) decoracion.setEstado(dto.getEstado());
    }
}
