package com.catolica.Artepax.cronograma.service.impl;

import com.catolica.Artepax.cronograma.dto.CronogramaRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUpdateDTO;
import com.catolica.Artepax.cronograma.entity.Cronograma;
import com.catolica.Artepax.cronograma.exception.CronogramaNotFoundException;
import com.catolica.Artepax.cronograma.mapper.CronogramaMapper;
import com.catolica.Artepax.cronograma.repository.CronogramaRepository;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CronogramaServiceImpl implements CronogramaService {

    private final CronogramaRepository cronogramaRepository;
    private final CronogramaMapper cronogramaMapper;
    private final EventoRepository eventoRepository;

    public CronogramaServiceImpl(CronogramaRepository cronogramaRepository,
                                 CronogramaMapper cronogramaMapper,
                                 EventoRepository eventoRepository) {
        this.cronogramaRepository = cronogramaRepository;
        this.cronogramaMapper = cronogramaMapper;
        this.eventoRepository = eventoRepository;
    }

    @Override
    @Transactional
    public CronogramaResponseDTO crear(CronogramaRequestDTO dto) {

        // validar que el evento existe
        Evento evento = eventoRepository.findById(dto.getIdEvento())
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + dto.getIdEvento()));

        // validar fechas
        if (dto.getFechaInicio().isAfter(dto.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        Cronograma cronograma = cronogramaMapper.toEntity(dto, evento);
        return cronogramaMapper.toDTO(cronogramaRepository.save(cronograma));
    }

    @Override
    public List<CronogramaResponseDTO> obtenerPorEvento(Long idEvento) {
        return cronogramaRepository.findByEventoId(idEvento)
                .stream()
                .map(cronogramaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CronogramaResponseDTO obtenerPorId(Long id) {
        Cronograma cronograma = cronogramaRepository.findById(id)
                .orElseThrow(() -> new CronogramaNotFoundException(id));
        return cronogramaMapper.toDTO(cronograma);
    }

    @Override
    @Transactional
    public CronogramaResponseDTO actualizar(Long id, CronogramaUpdateDTO dto) {

        Cronograma cronograma = cronogramaRepository.findById(id)
                .orElseThrow(() -> new CronogramaNotFoundException(id));

        if (dto.getActividad() != null) cronograma.setActividad(dto.getActividad());
        if (dto.getFechaInicio() != null) cronograma.setFechaInicio(dto.getFechaInicio());
        if (dto.getFechaFin() != null) cronograma.setFechaFin(dto.getFechaFin());

        LocalDateTime inicio = cronograma.getFechaInicio();
        LocalDateTime fin = cronograma.getFechaFin();
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        return cronogramaMapper.toDTO(cronogramaRepository.save(cronograma));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!cronogramaRepository.existsById(id)) {
            throw new CronogramaNotFoundException(id);
        }
        cronogramaRepository.deleteById(id);
    }
}