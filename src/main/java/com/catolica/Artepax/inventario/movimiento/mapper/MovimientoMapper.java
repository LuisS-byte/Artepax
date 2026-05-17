package com.catolica.Artepax.inventario.movimiento.mapper;

import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import org.springframework.stereotype.Component;

/**
 * Mapper between {@link MovimientoInventario} entity and {@link MovimientoResponseDTO}.
 */
@Component
public class MovimientoMapper {

    /**
     * Converts a {@link MovimientoInventario} entity into a response DTO.
     * Includes product ID and name, event ID and client name (when available),
     * movement type, origin, quantity, date, and reason.
     *
     * @param movimiento the movement entity to convert
     * @return a fully populated response DTO
     */
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
