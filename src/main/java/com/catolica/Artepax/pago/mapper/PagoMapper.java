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
    dto.setMetodo(pago.getMetodo());
    dto.setEventoId(pago.getEvento().getId());

    // si tiene fecha es PAGADO, si no es PENDIENTE
    dto.setEstadoPago(pago.getFecha() != null ? "PAGADO" : "PENDIENTE");

    return dto;
}
}