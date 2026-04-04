package com.catolica.Artepax.pago.service.impl;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.entity.Pago;
import com.catolica.Artepax.pago.mapper.PagoMapper;
import com.catolica.Artepax.pago.repository.PagoRepository;
import com.catolica.Artepax.pago.service.PagoService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PagoMapper pagoMapper;

    @Override
    @Transactional
    public void generarPagoAutomatico(Evento evento) {
        Pago pago = new Pago();
        pago.setEvento(evento);
        pago.setMonto(evento.getDecoracion().getPrecio());
        pago.setMetodo("EFECTIVO");
        pago.setFecha(null); // pendiente hasta que se confirme
        pagoRepository.save(pago);
    }

    @Override
    @Transactional
    public PagoResponseDTO confirmarPago(Long eventoId) {

        Pago pago = pagoRepository.findFirstByEventoId(eventoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado para evento ID: " + eventoId));

        if (pago.getFecha() != null) {
            throw new RuntimeException("El pago ya fue confirmado");
        }

        pago.setFecha(LocalDate.now());
        pagoRepository.save(pago);

        return pagoMapper.toDTO(pago);
    }

    @Override
    public List<PagoResponseDTO> obtenerPorEvento(Long eventoId) {
        return pagoRepository.findByEventoId(eventoId)
                .stream()
                .map(pagoMapper::toDTO)
                .toList();
    }
}