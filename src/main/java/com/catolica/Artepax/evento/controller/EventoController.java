package com.catolica.Artepax.evento.controller;

import com.catolica.Artepax.evento.dto.EventoProductoDTO;
import com.catolica.Artepax.evento.dto.CalendarioEventoDTO;
import com.catolica.Artepax.evento.dto.CancelacionEventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoExtraRequestDTO;
import com.catolica.Artepax.evento.dto.EventoRequestDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.dto.EventoResumenInternoDTO;
import com.catolica.Artepax.evento.dto.EventoUpdateDTO;
import com.catolica.Artepax.evento.dto.RentabilidadEventoDTO;
import com.catolica.Artepax.evento.model.VistaRentabilidadEvento;
import com.catolica.Artepax.evento.repository.VistaRentabilidadEventoRepository;
import com.catolica.Artepax.evento.service.EventoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * REST controller para gestión de eventos y su ciclo de vida.
 */
@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EventoController {

    private static final Logger log = LoggerFactory.getLogger(EventoController.class);

    private final EventoService eventoService;
    private final VistaRentabilidadEventoRepository rentabilidadRepository;

    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(
            @Valid @RequestBody EventoRequestDTO requestDTO) {
        log.info("POST /api/eventos clienteId={} decoracionId={}",
                requestDTO.getClienteId(), requestDTO.getDecoracionId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventoService.crearEvento(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<EventoResponseDTO>> obtenerEventos() {
        return ResponseEntity.ok(eventoService.obtenerEventos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> obtenerEventoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerEventoPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> actualizarEvento(
            @PathVariable Long id,
            @Valid @RequestBody EventoUpdateDTO updateDTO) {
        log.info("PUT /api/eventos/{}", id);
        return ResponseEntity.ok(eventoService.actualizarEvento(id, updateDTO));
    }

    @PatchMapping("/{id}/extras")
    public ResponseEntity<EventoResponseDTO> agregarExtras(
            @PathVariable Long id,
            @Valid @RequestBody EventoExtraRequestDTO requestDTO) {
        log.info("PATCH /api/eventos/{}/extras items={}", id, requestDTO.getItems().size());
        return ResponseEntity.ok(eventoService.agregarExtras(id, requestDTO));
    }

    @PatchMapping("/{id}/programar")
    public ResponseEntity<EventoResponseDTO> programarEvento(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/programar", id);
        return ResponseEntity.ok(eventoService.programarEvento(id));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<EventoResponseDTO> confirmarEvento(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/confirmar", id);
        return ResponseEntity.ok(eventoService.confirmarEvento(id));
    }

    @PatchMapping("/{id}/iniciar-montaje")
    public ResponseEntity<EventoResponseDTO> iniciarMontaje(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/iniciar-montaje", id);
        return ResponseEntity.ok(eventoService.iniciarMontaje(id));
    }

    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<EventoResponseDTO> finalizarEvento(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/finalizar", id);
        return ResponseEntity.ok(eventoService.finalizarEvento(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<EventoResponseDTO> iniciarCancelacion(
            @PathVariable Long id,
            @Valid @RequestBody CancelacionEventoRequestDTO body) {
        log.info("PATCH /api/eventos/{}/cancelar motivo={}", id, body.getMotivo());
        return ResponseEntity.ok(eventoService.iniciarCancelacion(id, body.getMotivo()));
    }

    @PatchMapping("/{id}/revertir-cancelacion")
    public ResponseEntity<EventoResponseDTO> revertirCancelacion(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/revertir-cancelacion", id);
        return ResponseEntity.ok(eventoService.revertirCancelacion(id));
    }

    @PatchMapping("/{id}/confirmar-cancelacion")
    public ResponseEntity<Void> confirmarCancelacion(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/confirmar-cancelacion", id);
        eventoService.confirmarCancelacion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Boolean> verificarDisponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam LocalTime horaInicio,
            @RequestParam LocalTime horaFin,
            @RequestParam String ubicacion,
            @RequestParam(required = false) Long eventoIdExcluir) {
        return ResponseEntity.ok(
                eventoService.verificarDisponibilidad(
                        fecha, horaInicio, horaFin, ubicacion, eventoIdExcluir));
    }

    @GetMapping("/calendario")
    public ResponseEntity<List<CalendarioEventoDTO>> obtenerCalendario(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fin) {
        return ResponseEntity.ok(eventoService.obtenerCalendario(inicio, fin));
    }

    @GetMapping("/{id}/rentabilidad")
    public ResponseEntity<RentabilidadEventoDTO> getRentabilidad(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.getRentabilidad(id));
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<EventoResumenInternoDTO> getDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.getResumenInterno(id));
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<List<EventoProductoDTO>> obtenerProductos(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerProductosPorEvento(id));
    }
}
