package com.catolica.Artepax.evento.controller;

import com.catolica.Artepax.evento.dto.*;
import com.catolica.Artepax.evento.model.VistaRentabilidadEvento;
import com.catolica.Artepax.evento.repository.VistaRentabilidadEventoRepository;
import com.catolica.Artepax.evento.service.EventoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EventoController {

    private final EventoService eventoService;

    private final VistaRentabilidadEventoRepository vistaRentabilidadRepo;

    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(
            @Valid @RequestBody EventoRequestDTO requestDTO) {
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
        return ResponseEntity.ok(eventoService.actualizarEvento(id, updateDTO));
    }

    // ─── MÁQUINA DE ESTADOS ───────────────────────────────────────────────────

    // COTIZACION → PROGRAMADO (reserva stock)
    @PatchMapping("/{id}/programar")
    public ResponseEntity<EventoResponseDTO> programarEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.programarEvento(id));
    }

    // PROGRAMADO → CONFIRMADO (manual, sin esperar anticipo)
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<EventoResponseDTO> confirmarEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.confirmarEvento(id));
    }

    // CONFIRMADO → EN_MONTAJE
    @PatchMapping("/{id}/iniciar-montaje")
    public ResponseEntity<EventoResponseDTO> iniciarMontaje(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.iniciarMontaje(id));
    }

    // EN_MONTAJE → FINALIZADO (genera movimientos SALIDA/EVENTO)
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<EventoResponseDTO> finalizarEvento(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.finalizarEvento(id));
    }

    // * → CANCELACION_PENDIENTE (inicia tiempo de gracia)
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<EventoResponseDTO> iniciarCancelacion(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String motivo = body.getOrDefault("motivo", "Sin motivo especificado");
        return ResponseEntity.ok(eventoService.iniciarCancelacion(id, motivo));
    }

    // CANCELACION_PENDIENTE → estado anterior (revertir dentro del tiempo de gracia)
    @PatchMapping("/{id}/revertir-cancelacion")
    public ResponseEntity<EventoResponseDTO> revertirCancelacion(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.revertirCancelacion(id));
    }

    // CANCELACION_PENDIENTE → CANCELADO (confirmar cancelación definitiva)
    @PatchMapping("/{id}/confirmar-cancelacion")
    public ResponseEntity<Void> confirmarCancelacion(@PathVariable Long id) {
        eventoService.confirmarCancelacion(id);
        return ResponseEntity.noContent().build();
    }

    // ─── CONSULTAS ────────────────────────────────────────────────────────────

    @GetMapping("/disponibilidad")
    public ResponseEntity<Boolean> verificarDisponibilidad(
            @RequestParam LocalDate fecha,
            @RequestParam LocalTime horaInicio,
            @RequestParam String ubicacion) {
        return ResponseEntity.ok(
                eventoService.verificarDisponibilidad(fecha, horaInicio, ubicacion));
    }

    @GetMapping("/calendario")
    public ResponseEntity<List<CalendarioEventoDTO>> obtenerCalendario(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fin) {
        return ResponseEntity.ok(eventoService.obtenerCalendario(inicio, fin));
    }

    @GetMapping("/{id}/rentabilidad")
    public ResponseEntity<RentabilidadEventoDTO> getRentabilidad(@PathVariable Long id) {
        VistaRentabilidadEvento vista = vistaRentabilidadRepo.findByIdEvento(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró rentabilidad para el evento " + id
                ));

        RentabilidadEventoDTO dto = new RentabilidadEventoDTO(
                vista.getIdEvento(),
                vista.getFechaEvento(),
                vista.getEstado(),
                vista.getPrecioAcordado(),
                vista.getIngresoCobrado(),
                vista.getCostoMateriales(),
                vista.getCostoManoObra(),
                vista.getGastosDesembolsados(),
                vista.getGastosProyectados(),
                vista.getGananciaNeta()
        );

        return ResponseEntity.ok(dto);
    }
}