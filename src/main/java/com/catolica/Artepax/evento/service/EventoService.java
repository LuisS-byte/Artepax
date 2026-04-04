package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventoService {

    EventoResponseDTO crearEvento(EventoRequestDTO requestDTO);

    List<EventoResponseDTO> obtenerEventos();

    EventoResponseDTO obtenerEventoPorId(Long id);

    EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO updateDTO);

    void eliminarEvento(Long id);

    void cancelarEvento(Long id, String motivo);

    void finalizarEvento(Long id);

    boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, String ubicacion);
    List<EventoResponseDTO> obtenerCalendario(LocalDate inicio, LocalDate fin);
}