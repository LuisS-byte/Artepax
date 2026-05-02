package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventoService {

    EventoResponseDTO crearEvento(EventoRequestDTO requestDTO);

    List<EventoResponseDTO> obtenerEventos();

    EventoResponseDTO obtenerEventoPorId(Long id);

    EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO updateDTO);

    boolean verificarDisponibilidad(LocalDate fecha, LocalTime horaInicio, String ubicacion);
    List<CalendarioEventoDTO> obtenerCalendario(LocalDate inicio, LocalDate fin);

    EventoResponseDTO programarEvento(Long id);
    EventoResponseDTO iniciarMontaje(Long id);
    EventoResponseDTO finalizarEvento(Long id);
    EventoResponseDTO iniciarCancelacion(Long id, String motivo);
    EventoResponseDTO revertirCancelacion(Long id);
    void confirmarCancelacion(Long id);
    void confirmarDesdeAnticipo(Long id);

    EventoResponseDTO confirmarEvento(Long id);

    RentabilidadEventoDTO getRentabilidad(Long id);
}