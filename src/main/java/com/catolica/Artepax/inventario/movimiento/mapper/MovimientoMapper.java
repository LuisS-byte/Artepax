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
        if (movimiento.getEvento() != null) {
            dto.setEventoId(movimiento.getEvento().getId());
        }
        dto.setTipoMovimiento(movimiento.getTipoMovimiento());
        dto.setTipoOrigen(movimiento.getTipoOrigen());
        dto.setCantidad(movimiento.getCantidad());
        dto.setFecha(movimiento.getFecha());
        dto.setMotivo(movimiento.getMotivo());

        if (movimiento.getProducto() != null) {
            dto.setProductoNombre(movimiento.getProducto().getNombre());
        }
        if (movimiento.getEvento() != null) {
            dto.setEventoNombre(movimiento.getEvento().getCliente().getNombre());
        }

        return dto;
    }
}
