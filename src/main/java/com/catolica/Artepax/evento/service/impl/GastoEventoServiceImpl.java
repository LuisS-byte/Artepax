package com.catolica.Artepax.evento.service.impl;
import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.GastoEvento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.GastoEventoRepository;
import com.catolica.Artepax.evento.service.GastoEventoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GastoEventoServiceImpl implements GastoEventoService {

    private final GastoEventoRepository gastoRepository;
    private final EventoRepository eventoRepository;

    public GastoEventoServiceImpl(GastoEventoRepository gastoRepository,
                                  EventoRepository eventoRepository) {
        this.gastoRepository = gastoRepository;
        this.eventoRepository = eventoRepository;
    }

    @Override
    @Transactional
    public GastoEventoResponseDTO crear(GastoEventoRequestDTO dto) {
        Evento evento = eventoRepository.findById(dto.getEventoId())
                .orElseThrow(() -> new EventoException(
                        "Evento no encontrado: " + dto.getEventoId()));

        // Solo en estados activos — según DDL
        String estado = evento.getEstadoEvento().getNombre();
        if (!List.of("PROGRAMADO", "CONFIRMADO", "EN_MONTAJE").contains(estado)) {
            throw new IllegalArgumentException(
                    "Solo se pueden registrar gastos en eventos PROGRAMADO, " +
                            "CONFIRMADO o EN_MONTAJE. Estado actual: " + estado);
        }

        // Validar estado del gasto
        String gastoEstado = dto.getGastoEstado() != null ? dto.getGastoEstado() : "PROYECTADO";
        if (!List.of("PROYECTADO", "DESEMBOLSADO").contains(gastoEstado)) {
            throw new IllegalArgumentException("Estado de gasto inválido: " + gastoEstado);
        }

        GastoEvento gasto = new GastoEvento();
        gasto.setEvento(evento);
        gasto.setGastoConcepto(dto.getGastoConcepto());
        gasto.setGastoMonto(dto.getGastoMonto());
        gasto.setGastoEstado(gastoEstado);
        gasto.setGastoNota(dto.getGastoNota());

        return toDTO(gastoRepository.save(gasto));
    }

    @Override
    public List<GastoEventoResponseDTO> obtenerPorEvento(Long eventoId) {
        return gastoRepository.findByEventoId(eventoId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public GastoEventoResponseDTO obtenerPorId(Long id) {
        return toDTO(obtenerOFallar(id));
    }

    @Override
    @Transactional
    public GastoEventoResponseDTO desembolsar(Long id) {
        GastoEvento gasto = obtenerOFallar(id);

        if (gasto.getGastoEstado().equals("DESEMBOLSADO")) {
            throw new IllegalArgumentException("El gasto ya fue desembolsado");
        }

        gasto.setGastoEstado("DESEMBOLSADO");
        return toDTO(gastoRepository.save(gasto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        GastoEvento gasto = obtenerOFallar(id);

        if (gasto.getGastoEstado().equals("DESEMBOLSADO")) {
            throw new IllegalArgumentException(
                    "No se puede eliminar un gasto ya desembolsado");
        }

        gastoRepository.deleteById(id);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────
    private GastoEvento obtenerOFallar(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gasto no encontrado: " + id));
    }

    private GastoEventoResponseDTO toDTO(GastoEvento g) {
        GastoEventoResponseDTO dto = new GastoEventoResponseDTO();
        dto.setId(g.getId());
        dto.setEventoId(g.getEvento().getId());
        dto.setGastoConcepto(g.getGastoConcepto());
        dto.setGastoMonto(g.getGastoMonto());
        dto.setGastoEstado(g.getGastoEstado());
        dto.setGastoFecha(g.getGastoFecha());
        dto.setGastoNota(g.getGastoNota());
        return dto;
    }
}