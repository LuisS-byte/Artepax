package com.catolica.Artepax.inventario.movimiento.mapper;

import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import org.springframework.stereotype.Component;

@Component
public class MovimientoMapper {

    public MovimientoResponseDTO toDTO(MovimientoInventario movimiento) {
        MovimientoResponseDTO dto = new MovimientoResponseDTO();
        dto.setId(movimiento.getId());
        dto.setProductoId(movimiento.getProducto().getId());
        dto.setTipoMovimiento(movimiento.getTipoMovimiento());
        dto.setCantidad(movimiento.getCantidad());
        dto.setFecha(movimiento.getFecha());
        dto.setMotivo(movimiento.getMotivo());
        return dto;
    }
}