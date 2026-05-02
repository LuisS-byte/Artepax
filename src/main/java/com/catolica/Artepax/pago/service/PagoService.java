package com.catolica.Artepax.pago.service;

import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;

import java.util.List;


public interface PagoService {

    PagoResponseDTO registrarPago(PagoRequestDTO dto);
    PagoResponseDTO confirmarPago(Long pagoId);
    PagoResponseDTO anularPago(Long pagoId);
    List<PagoResponseDTO> obtenerPorEvento(Long eventoId);


    ResumenPagoEventoDTO getResumenPagos(Long eventoId);
}
