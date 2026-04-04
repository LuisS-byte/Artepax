package com.catolica.Artepax.evento.mapper;

import com.catolica.Artepax.evento.dto.*;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.clientes.entity.Cliente;

import org.springframework.stereotype.Component;
@Component
public class EventoMapper {

    public Evento toEntity(EventoRequestDTO dto, Cliente cliente) {
        Evento evento = new Evento();

        evento.setCliente(cliente);
        evento.setFecha(dto.getFecha());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setUbicacion(dto.getUbicacion());
        evento.setDecoracionId(dto.getDecoracionId());
        evento.setCapacidad(dto.getCapacidad());
        evento.setDescripcion(dto.getDescripcion());

        return evento;
    }

    public EventoResponseDTO toResponseDTO(Evento evento) {
        EventoResponseDTO dto = new EventoResponseDTO();

        dto.setId(evento.getId());
        dto.setFecha(evento.getFecha());
        dto.setHoraInicio(evento.getHoraInicio());
        dto.setHoraFin(evento.getHoraFin());
        dto.setUbicacion(evento.getUbicacion());
        dto.setCapacidad(evento.getCapacidad());
        dto.setDescripcion(evento.getDescripcion());
        dto.setEstadoEventoId(evento.getEstadoEventoId());

        if (evento.getCliente() != null) {
            dto.setClienteId(evento.getCliente().getId());
            dto.setClienteNombre(evento.getCliente().getNombre());
        }

        return dto;
    }

    public void updateEntityFromDTO(Evento evento, EventoUpdateDTO dto, Cliente cliente) {

        if (dto.getFecha() != null) evento.setFecha(dto.getFecha());
        if (dto.getHoraInicio() != null) evento.setHoraInicio(dto.getHoraInicio());
        if (dto.getHoraFin() != null) evento.setHoraFin(dto.getHoraFin());
        if (dto.getUbicacion() != null) evento.setUbicacion(dto.getUbicacion());
        if (dto.getDecoracionId() != null) evento.setDecoracionId(dto.getDecoracionId());
        if (dto.getCapacidad() != null) evento.setCapacidad(dto.getCapacidad());
        if (dto.getDescripcion() != null) evento.setDescripcion(dto.getDescripcion());

        if (cliente != null) evento.setCliente(cliente);
    }
}