package com.catolica.Artepax.evento.controller;

import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import com.catolica.Artepax.evento.service.GastoEventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller para gestión de gastos de eventos.
 */
@RestController
@RequestMapping("/api/gastos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GastoEventoController {

    private static final Logger log = LoggerFactory.getLogger(GastoEventoController.class);

    private final GastoEventoService gastoService;

    @PostMapping
    public ResponseEntity<GastoEventoResponseDTO> crear(
            @Valid @RequestBody GastoEventoRequestDTO dto) {
        log.info("POST /api/gastos eventoId={}", dto.getEventoId());
        return ResponseEntity.status(201).body(gastoService.crear(dto));
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<GastoEventoResponseDTO>> obtenerPorEvento(
            @PathVariable Long eventoId) {
        return ResponseEntity.ok(gastoService.obtenerPorEvento(eventoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GastoEventoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/desembolsar")
    public ResponseEntity<GastoEventoResponseDTO> desembolsar(@PathVariable Long id) {
        log.info("PATCH /api/gastos/{}/desembolsar", id);
        return ResponseEntity.ok(gastoService.desembolsar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/gastos/{}", id);
        gastoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<GastoEventoResponseDTO>> obtenerTodos(
            @PageableDefault(size = 50, sort = "gastoFecha") Pageable pageable) {
        return ResponseEntity.ok(gastoService.obtenerTodos(pageable));
    }
}
