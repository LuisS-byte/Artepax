package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;

import java.util.List;

public interface EventoService {

    EventoResponseDTO crearEvento(EventoRequestDTO requestDTO);

    List<EventoResponseDTO> obtenerEventos();

    EventoResponseDTO obtenerEventoPorId(Long id);

    EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO updateDTO);

    void eliminarEvento(Long id);
}