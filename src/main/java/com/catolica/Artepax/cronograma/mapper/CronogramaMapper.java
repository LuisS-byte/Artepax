package com.catolica.Artepax.cronograma.mapper;

import com.catolica.Artepax.cronograma.dto.CronogramaRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.entity.Cronograma;
import com.catolica.Artepax.evento.model.Evento;
import org.springframework.stereotype.Component;

@Component
public class CronogramaMapper {

    public Cronograma toEntity(CronogramaRequestDTO dto, Evento evento) {
        Cronograma cronograma = new Cronograma();
        cronograma.setEvento(evento);
        cronograma.setActividad(dto.getActividad());
        cronograma.setFechaInicio(dto.getFechaInicio());
        cronograma.setFechaFin(dto.getFechaFin());
        return cronograma;
    }

    public CronogramaResponseDTO toDTO(Cronograma cronograma) {
        CronogramaResponseDTO dto = new CronogramaResponseDTO();
        dto.setId(cronograma.getId());
        dto.setIdEvento(cronograma.getEvento().getId());
        dto.setActividad(cronograma.getActividad());
        dto.setFechaInicio(cronograma.getFechaInicio());
        dto.setFechaFin(cronograma.getFechaFin());
        return dto;
    }
}
