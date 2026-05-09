package com.catolica.Artepax.pago.mapper;

import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.entity.Pago;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoResponseDTO toDTO(Pago pago) {
        PagoResponseDTO dto = new PagoResponseDTO();
        dto.setId(pago.getId());
        dto.setMonto(pago.getMonto());
        dto.setFecha(pago.getFecha());
        dto.setEventoId(pago.getEvento() != null ? pago.getEvento().getId() : null);

        if (pago.getEstadoPago() != null) {
            dto.setEstadoPago(pago.getEstadoPago().getEstadoNombre());
        }
        if (pago.getConceptoPago() != null) {
            dto.setConceptoPago(pago.getConceptoPago().getConceptoNombre());
        }
        if (pago.getMetodoPago() != null) {
            dto.setMetodoPago(pago.getMetodoPago().getMetodoNombre());
        }

        if (pago.getEvento() != null && pago.getEvento().getCliente() != null) {
            dto.setClienteNombre(pago.getEvento().getCliente().getNombre());
        }

        if (pago.getEvento() != null && pago.getEvento().getEstadoEvento() != null) {
            dto.setEstadoEventoNombre(pago.getEvento().getEstadoEvento().getNombre());
        }

        return dto;
    }
}
