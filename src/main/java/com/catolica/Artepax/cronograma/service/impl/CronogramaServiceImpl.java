package com.catolica.Artepax.cronograma.service.impl;

import com.catolica.Artepax.cronograma.dto.CronogramaRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUpdateDTO;
import com.catolica.Artepax.cronograma.entity.Cronograma;
import com.catolica.Artepax.cronograma.exception.CronogramaNotFoundException;
import com.catolica.Artepax.cronograma.mapper.CronogramaMapper;
import com.catolica.Artepax.cronograma.repository.CronogramaRepository;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CronogramaServiceImpl implements CronogramaService {

    private final CronogramaRepository cronogramaRepository;
    private final CronogramaMapper cronogramaMapper;

    public CronogramaServiceImpl(CronogramaRepository cronogramaRepository,
                                  CronogramaMapper cronogramaMapper) {
        this.cronogramaRepository = cronogramaRepository;
        this.cronogramaMapper = cronogramaMapper;
    }

    @Override
    public CronogramaResponseDTO crear(CronogramaRequestDTO dto) {
        if (dto.getFechaInicio().isAfter(dto.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        Cronograma cronograma = cronogramaMapper.toEntity(dto);
        return cronogramaMapper.toDTO(cronogramaRepository.save(cronograma));
    }

    @Override
    public List<CronogramaResponseDTO> obtenerPorEvento(Long idEvento) {
        return cronogramaRepository.findByIdEvento(idEvento)
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
    public CronogramaResponseDTO actualizar(Long id, CronogramaUpdateDTO dto) {
        Cronograma cronograma = cronogramaRepository.findById(id)
                .orElseThrow(() -> new CronogramaNotFoundException(id));

        if (dto.getActividad() != null) {
            cronograma.setActividad(dto.getActividad());
        }
        if (dto.getFechaInicio() != null) {
            cronograma.setFechaInicio(dto.getFechaInicio());
        }
        if (dto.getFechaFin() != null) {
            cronograma.setFechaFin(dto.getFechaFin());
        }

        LocalDateTime inicio = cronograma.getFechaInicio();
        LocalDateTime fin = cronograma.getFechaFin();
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        return cronogramaMapper.toDTO(cronogramaRepository.save(cronograma));
    }

    @Override
    public void eliminar(Long id) {
        if (!cronogramaRepository.existsById(id)) {
            throw new CronogramaNotFoundException(id);
        }
        cronogramaRepository.deleteById(id);
    }
}
