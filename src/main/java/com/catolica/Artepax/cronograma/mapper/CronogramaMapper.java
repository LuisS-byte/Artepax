package com.catolica.Artepax.cronograma.mapper;

import com.catolica.Artepax.cronograma.dto.CronogramaRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.entity.Cronograma;
import com.catolica.Artepax.cronograma.service.policy.CronogramaPolicy;
import com.catolica.Artepax.evento.model.Evento;
import org.springframework.stereotype.Component;

/**
 * Mapper entre DTOs y entidades del módulo cronograma.
 */
@Component
public class CronogramaMapper {

    private final CronogramaPolicy cronogramaPolicy;

    public CronogramaMapper(CronogramaPolicy cronogramaPolicy) {
        this.cronogramaPolicy = cronogramaPolicy;
    }

    /**
     * Convierte un request DTO a entidad Cronograma.
     * El estado inicial se establece por defecto en la entidad (PENDIENTE).
     *
     * @param dto    datos de entrada de la actividad
     * @param evento evento al que pertenecerá la actividad
     * @return entidad Cronograma con los valores del DTO
     */
    public Cronograma toEntity(CronogramaRequestDTO dto, Evento evento) {
        Cronograma cronograma = new Cronograma();
        cronograma.setEvento(evento);
        cronograma.setActividad(dto.getActividad());
        cronograma.setFechaInicio(dto.getFechaInicio());
        cronograma.setFechaFin(dto.getFechaFin());
        return cronograma;
    }

    /**
     * Convierte una entidad Cronograma a response DTO.
     * Si el evento o sus relaciones son nulas, los campos denormalizados quedan null.
     *
     * @param cronograma entidad a convertir
     * @return DTO de respuesta con datos de la actividad y del evento
     */
    public CronogramaResponseDTO toDTO(Cronograma cronograma) {
        CronogramaResponseDTO dto = new CronogramaResponseDTO();
        dto.setId(cronograma.getId());
        dto.setActividad(cronograma.getActividad());
        dto.setFechaInicio(cronograma.getFechaInicio());
        dto.setFechaFin(cronograma.getFechaFin());
        dto.setCronoEstado(cronograma.getCronoEstado());
        dto.setCronoObservacion(cronograma.getCronoObservacion());
        dto.setEditable(cronogramaPolicy.isEditable(cronograma.getCronoEstado()));
        dto.setCancelable(cronogramaPolicy.isCancelable(cronograma.getCronoEstado()));
        dto.setAccionesPermitidas(cronogramaPolicy.getAccionesPermitidas(cronograma.getCronoEstado()));
        dto.setEstadosSiguientesPermitidos(
                cronogramaPolicy.getEstadosSiguientesPermitidos(cronograma.getCronoEstado()));

        Evento evento = cronograma.getEvento();
        if (evento != null) {
            dto.setIdEvento(evento.getId());
            if (evento.getCliente() != null) {
                dto.setClienteNombre(evento.getCliente().getNombre());
            }
            if (evento.getEstadoEvento() != null) {
                dto.setEstadoEventoNombre(evento.getEstadoEvento().getNombre());
            }
        }

        return dto;
    }
}
