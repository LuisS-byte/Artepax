package com.catolica.Artepax.pago.service;

import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.evento.model.Evento;

import java.util.List;


public interface PagoService {

    // llamado internamente al crear evento
    void generarPagoAutomatico(Evento evento);

    // confirmar pago recibido
    PagoResponseDTO confirmarPago(Long eventoId);

    // obtener pagos de un evento
    List<PagoResponseDTO> obtenerPorEvento(Long eventoId);
}
