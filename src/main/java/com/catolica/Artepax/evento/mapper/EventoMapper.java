package com.catolica.Artepax.evento.mapper;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.evento.dto.*;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import org.springframework.stereotype.Component;

@Component
public class EventoMapper {

    public Evento toEntity(EventoRequestDTO dto, Cliente cliente, Decoracion decoracion) {
        Evento evento = new Evento();
        evento.setCliente(cliente);
        evento.setDecoracion(decoracion);
        evento.setFecha(dto.getFecha());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setUbicacion(dto.getUbicacion());
        evento.setNota(dto.getNota());
        return evento;
    }

    public EventoResponseDTO toResponseDTO(Evento evento) {
        EventoResponseDTO dto = new EventoResponseDTO();
        dto.setId(evento.getId());
        dto.setFecha(evento.getFecha());
        dto.setHoraInicio(evento.getHoraInicio());
        dto.setHoraFin(evento.getHoraFin());
        dto.setUbicacion(evento.getUbicacion());
        dto.setNota(evento.getNota());
        dto.setNotaPrecio(evento.getNotaPrecio());
        dto.setMotivoCancelacion(evento.getMotivoCancelacion());
        dto.setFechaCreacion(evento.getFechaCreacion());
        dto.setPrecioBase(evento.getPrecioBase());
        dto.setPrecioAdicional(evento.getPrecioAdicional());
        dto.setPrecioTotal(evento.getPrecioTotal());
        dto.setClienteNotificado(evento.getClienteNotificado());
        dto.setFechaConfirmacion(evento.getFechaConfirmacion());
        dto.setFechaCancelacion(evento.getFechaCancelacion());
        dto.setFechaExpiracion(evento.getFechaExpiracion());

        if (evento.getCliente() != null) {
            dto.setClienteId(evento.getCliente().getId());
            dto.setClienteNombre(evento.getCliente().getNombre());
        }

        if (evento.getDecoracion() != null) {
            dto.setDecoracionId(evento.getDecoracion().getId());
            dto.setDecoracionNombre(evento.getDecoracion().getNombre());
        }

        if (evento.getEstadoEvento() != null) {
            dto.setEstadoEventoId(evento.getEstadoEvento().getId());
            dto.setEstadoEventoNombre(evento.getEstadoEvento().getNombre());
        }

        if (evento.getTipoEvento() != null) {
            dto.setTipoEventoId(evento.getTipoEvento().getId());
            dto.setTipoEventoNombre(evento.getTipoEvento().getTipoNombre());
        }

        return dto;
    }

    public void updateEntityFromDTO(Evento evento, EventoUpdateDTO dto,
                                    Cliente cliente, Decoracion decoracion) {
        if (dto.getFecha() != null) evento.setFecha(dto.getFecha());
        if (dto.getHoraInicio() != null) evento.setHoraInicio(dto.getHoraInicio());
        if (dto.getHoraFin() != null) evento.setHoraFin(dto.getHoraFin());
        if (dto.getUbicacion() != null) evento.setUbicacion(dto.getUbicacion());
        if (dto.getNota() != null) evento.setNota(dto.getNota());
        if (cliente != null) evento.setCliente(cliente);
        if (decoracion != null) evento.setDecoracion(decoracion);
    }
}
