package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.mapper.EventoMapper;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.service.EventoService;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.repository.ClienteRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final EventoMapper eventoMapper;

    /**
     * Crear evento
     */
    @Override
public EventoResponseDTO crearEvento(EventoRequestDTO dto) {

    // validar cliente
    Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new EventoException("Cliente no encontrado"));

    // validar fecha
    if (dto.getFecha().isBefore(LocalDate.now())) {
        throw new EventoException("No se puede crear un evento en el pasado");
    }

    // validar conflicto
    boolean existe = eventoRepository.existsByFechaAndHoraInicioAndUbicacion(
            dto.getFecha(),
            dto.getHoraInicio(),
            dto.getUbicacion()
    );

    if (existe) {
        throw new EventoException("Ya existe un evento en esa fecha, hora y ubicación");
    }

    // mapear
    Evento evento = eventoMapper.toEntity(dto, cliente);

    // 🔥 AQUÍ VA (ANTES DE GUARDAR)
    evento.setEstadoEventoId(1L); // PROGRAMADO

    evento.setFechaCreacion(LocalDate.now());

    // guardar
    Evento guardado = eventoRepository.save(evento);

    return eventoMapper.toResponseDTO(guardado);
}

    /**
     * Obtener todos los eventos
     */
    @Override
    public List<EventoResponseDTO> obtenerEventos() {
        return eventoRepository.findAll()
                .stream()
                .map(eventoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener evento por ID
     */
    @Override
    public EventoResponseDTO obtenerEventoPorId(Long id) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + id));

        return eventoMapper.toResponseDTO(evento);
    }

    /**
     * Actualizar evento
     */
    @Override
    public EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO dto) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + id));

        Cliente cliente = null;

        // cliente opcional
        if (dto.getClienteId() != null) {
            cliente = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new EventoException("Cliente no encontrado"));
        }

        // validar fecha
        if (dto.getFecha() != null && dto.getFecha().isBefore(LocalDate.now())) {
            throw new EventoException("No se puede actualizar a una fecha pasada");
        }

        // validar conflicto si cambian datos clave
        if (dto.getFecha() != null || dto.getHoraInicio() != null || dto.getUbicacion() != null) {

            LocalDate fecha = dto.getFecha() != null ? dto.getFecha() : evento.getFecha();
            var horaInicio = dto.getHoraInicio() != null ? dto.getHoraInicio() : evento.getHoraInicio();
            var ubicacion = dto.getUbicacion() != null ? dto.getUbicacion() : evento.getUbicacion();

            boolean existe = eventoRepository.existsByFechaAndHoraInicioAndUbicacion(
                    fecha,
                    horaInicio,
                    ubicacion
            );

            if (existe && !(evento.getFecha().equals(fecha)
                    && evento.getHoraInicio().equals(horaInicio)
                    && evento.getUbicacion().equals(ubicacion))) {

                throw new EventoException("Conflicto con otro evento existente");
            }
        }

        // actualizar con mapper
        eventoMapper.updateEntityFromDTO(evento, dto, cliente);

        Evento actualizado = eventoRepository.save(evento);

        return eventoMapper.toResponseDTO(actualizado);
    }

    /**
     * Eliminar evento (lógico)
     */
    @Override
    public void eliminarEvento(Long id) {

        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException("Evento no encontrado con ID: " + id));

        // eliminación lógica (en tu BD sería id_estadoevento, aquí simple)
        evento.setMotivoCancelacion("Evento cancelado");

        eventoRepository.save(evento);
    }
}