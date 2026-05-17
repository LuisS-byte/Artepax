package com.catolica.Artepax.pago.mapper;

import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.entity.Pago;
import org.springframework.stereotype.Component;

/**
 * Mapper between {@link Pago} entity and {@link PagoResponseDTO}.
 */
@Component
public class PagoMapper {

    /**
     * Converts a {@link Pago} entity to its response DTO.
     * <p>
     * Resolves relationships (evento, estadoPago, conceptoPago, metodoPago, cliente)
     * with null-safe accessors and maps display names instead of entity references.
     *
     * @param pago the payment entity to convert (must not be {@code null})
     * @return the populated response DTO
     */
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
