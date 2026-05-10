package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import com.catolica.Artepax.evento.dto.ResumenGastoEventoDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.exception.EventoNotFoundException;
import com.catolica.Artepax.evento.exception.GastoEventoException;
import com.catolica.Artepax.evento.exception.GastoEventoNotFoundException;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.GastoEvento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.GastoEventoRepository;
import com.catolica.Artepax.evento.service.GastoEventoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.catolica.Artepax.evento.model.EstadoEventoNames.ESTADOS_PERMITIDOS_GASTOS;
import static com.catolica.Artepax.evento.model.GastoEstadoNames.DESEMBOLSADO;
import static com.catolica.Artepax.evento.model.GastoEstadoNames.PROYECTADO;

@Service
public class GastoEventoServiceImpl implements GastoEventoService {

    private static final Logger log = LoggerFactory.getLogger(GastoEventoServiceImpl.class);

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
                .orElseThrow(() -> new EventoNotFoundException(dto.getEventoId()));

        String estado = evento.getEstadoEvento().getNombre();
        if (!ESTADOS_PERMITIDOS_GASTOS.contains(estado)) {
            throw new GastoEventoException(
                    "Solo se pueden registrar gastos en eventos PROGRAMADO, " +
                            "CONFIRMADO o EN_MONTAJE. Estado actual: " + estado);
        }

        String gastoEstado = dto.getGastoEstado() != null ? dto.getGastoEstado() : PROYECTADO;
        if (!List.of(PROYECTADO, DESEMBOLSADO).contains(gastoEstado)) {
            throw new GastoEventoException("Estado de gasto inválido: " + gastoEstado);
        }

        GastoEvento gasto = new GastoEvento();
        gasto.setEvento(evento);
        gasto.setGastoConcepto(dto.getGastoConcepto());
        gasto.setGastoMonto(dto.getGastoMonto());
        gasto.setGastoEstado(gastoEstado);
        gasto.setGastoNota(dto.getGastoNota());

        GastoEvento guardado = gastoRepository.save(gasto);
        log.info("gasto_creado id={} evento={} monto={} estado={}",
                guardado.getId(), dto.getEventoId(), dto.getGastoMonto(), gastoEstado);

        return toDTO(guardado);
    }

    @Override
    public List<GastoEventoResponseDTO> obtenerPorEvento(Long eventoId) {
        return gastoRepository.findByEventoId(eventoId)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public Page<GastoEventoResponseDTO> obtenerTodos(Pageable pageable) {
        return gastoRepository.findAllWithEvento(pageable)
                .map(this::toDTO);
    }

    @Override
    public GastoEventoResponseDTO obtenerPorId(Long id) {
        return toDTO(obtenerOFallar(id));
    }

    @Override
    @Transactional
    public GastoEventoResponseDTO desembolsar(Long id) {
        GastoEvento gasto = obtenerOFallar(id);

        if (gasto.getGastoEstado().equals(DESEMBOLSADO)) {
            throw new GastoEventoException("El gasto ya fue desembolsado");
        }

        gasto.setGastoEstado(DESEMBOLSADO);
        GastoEvento guardado = gastoRepository.save(gasto);
        log.info("gasto_desembolsado id={}", id);

        return toDTO(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        GastoEvento gasto = obtenerOFallar(id);

        if (gasto.getGastoEstado().equals(DESEMBOLSADO)) {
            throw new GastoEventoException(
                    "No se puede eliminar un gasto ya desembolsado");
        }

        gastoRepository.deleteById(id);
        log.info("gasto_eliminado id={}", id);
    }

    private GastoEvento obtenerOFallar(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new GastoEventoNotFoundException(id));
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

        if (g.getEvento() != null) {
            dto.setClienteNombre(g.getEvento().getCliente().getNombre());
            dto.setEstadoEventoNombre(g.getEvento().getEstadoEvento().getNombre());
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenGastoEventoDTO getResumen(Long eventoId) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new EventoNotFoundException(eventoId);
        }

        BigDecimal totalProyectado = gastoRepository.sumMontoByEventoIdAndEstado(eventoId, PROYECTADO);
        BigDecimal totalDesembolsado = gastoRepository.sumMontoByEventoIdAndEstado(eventoId, DESEMBOLSADO);
        BigDecimal totalPendiente = totalProyectado;

        return new ResumenGastoEventoDTO(totalProyectado, totalDesembolsado, totalPendiente);
    }
}
