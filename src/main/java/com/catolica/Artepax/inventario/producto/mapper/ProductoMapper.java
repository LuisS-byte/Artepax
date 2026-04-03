package com.catolica.Artepax.inventario.producto.mapper;

import com.catolica.Artepax.inventario.producto.dto.ProductoRequestDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoUpdateDTO;
import com.catolica.Artepax.inventario.producto.entity.Producto;

import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    // 🔹 DTO → Entity (crear producto)
    public Producto toEntity(ProductoRequestDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setCantidadTotal(dto.getCantidadTotal());
        producto.setEstado(dto.getEstado());
        return producto;
    }

    // 🔹 Entity → DTO (respuesta)
    public ProductoResponseDTO toDTO(Producto producto) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setCantidadTotal(producto.getCantidadTotal());
        dto.setEstado(producto.getEstado());
        dto.setFechaCreacion(producto.getFechaCreacion());
        return dto;
    }

    // 🔥 Update limpio (muy importante)
    public void updateEntity(Producto producto, ProductoUpdateDTO dto) {
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setEstado(dto.getEstado());
    }
}