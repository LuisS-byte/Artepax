package com.catolica.Artepax.inventario.decoracion.mapper;


import com.catolica.Artepax.inventario.decoracion.dto.DecoracionProductoResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DecoracionMapper {

    public Decoracion toEntity(DecoracionRequestDTO dto) {
        Decoracion decoracion = new Decoracion();
        decoracion.setNombre(dto.getNombre());
        decoracion.setDescripcion(dto.getDescripcion());
        decoracion.setPrecio(dto.getPrecio());
        decoracion.setEstado(dto.getEstado());
        return decoracion;
    }

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

    public DecoracionProductoResponseDTO toProductoDTO(DecoracionProducto dp) {
        DecoracionProductoResponseDTO dto = new DecoracionProductoResponseDTO();
        dto.setProductoId(dp.getProducto().getId());
        dto.setProductoNombre(dp.getProducto().getNombre());
        dto.setCantidadRequerida(dp.getCantidadRequerida());
        return dto;
    }

    public void updateEntity(Decoracion decoracion, DecoracionUpdateDTO dto) {
        if (dto.getNombre() != null) decoracion.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) decoracion.setDescripcion(dto.getDescripcion());
        if (dto.getPrecio() != null) decoracion.setPrecio(dto.getPrecio());
        if (dto.getEstado() != null) decoracion.setEstado(dto.getEstado());
    }
}
