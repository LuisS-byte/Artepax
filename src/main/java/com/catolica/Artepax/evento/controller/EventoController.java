package com.catolica.Artepax.evento.controller;

import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;
import com.catolica.Artepax.evento.service.EventoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    /**
     * Crear evento
     */
    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(
            @Valid @RequestBody EventoRequestDTO requestDTO) {

        EventoResponseDTO response = eventoService.crearEvento(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Obtener todos los eventos
     */
    @GetMapping
    public ResponseEntity<List<EventoResponseDTO>> obtenerEventos() {

        List<EventoResponseDTO> eventos = eventoService.obtenerEventos();
        return ResponseEntity.ok(eventos);
    }

    /**
     * Obtener evento por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> obtenerEventoPorId(
            @PathVariable Long id) {

        EventoResponseDTO evento = eventoService.obtenerEventoPorId(id);
        return ResponseEntity.ok(evento);
    }

    /**
     * Actualizar evento
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> actualizarEvento(
            @PathVariable Long id,
            @Valid @RequestBody EventoUpdateDTO updateDTO) {

        EventoResponseDTO eventoActualizado = eventoService.actualizarEvento(id, updateDTO);
        return ResponseEntity.ok(eventoActualizado);
    }

    /**
     * Eliminar evento (lógico o físico según implementación)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEvento(
            @PathVariable Long id) {

        eventoService.eliminarEvento(id);
        return ResponseEntity.noContent().build();
    }

    // PUT /eventos/{id}/cancelar
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarEvento(
            @PathVariable Long id,
            @RequestParam String motivo) {

        eventoService.cancelarEvento(id, motivo);
        return ResponseEntity.noContent().build();
    }

    // PUT /eventos/{id}/finalizar
    @PutMapping("/{id}/finalizar")
    public ResponseEntity<Void> finalizarEvento(@PathVariable Long id) {
        eventoService.finalizarEvento(id);
        return ResponseEntity.noContent().build();
    }

    // GET /eventos/disponibilidad?fecha=2026-05-01&horaInicio=10:00&ubicacion=Salon
    @GetMapping("/disponibilidad")
    public ResponseEntity<Boolean> verificarDisponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam LocalTime horaInicio,
            @RequestParam String ubicacion) {

        boolean disponible = eventoService.verificarDisponibilidad(fecha, horaInicio, ubicacion);
        return ResponseEntity.ok(disponible);
    }

    // GET /eventos/calendario?inicio=2026-05-01&fin=2026-05-31
    @GetMapping("/calendario")
    public ResponseEntity<List<EventoResponseDTO>> obtenerCalendario(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fin) {

        List<EventoResponseDTO> eventos = eventoService.obtenerCalendario(inicio, fin);
        return ResponseEntity.ok(eventos);
    }
}