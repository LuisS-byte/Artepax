package com.catolica.Artepax.pago.service;

import com.catolica.Artepax.pago.dto.EventoCobranzaDTO;
import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;


public interface PagoService {

    PagoResponseDTO registrarPago(PagoRequestDTO dto);
    PagoResponseDTO confirmarPago(Long pagoId);
    PagoResponseDTO anularPago(Long pagoId);
    List<PagoResponseDTO> obtenerPorEvento(Long eventoId);

    Page<PagoResponseDTO> obtenerTodos(Pageable pageable);

    ResumenPagoEventoDTO getResumenPagos(Long eventoId);

    List<EventoCobranzaDTO> getEventosCobranza(Map<String, Object> filtros);
}
