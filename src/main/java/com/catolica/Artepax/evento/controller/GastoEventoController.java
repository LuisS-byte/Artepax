package com.catolica.Artepax.evento.controller;
import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import com.catolica.Artepax.evento.service.GastoEventoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gastos")
@PreAuthorize("hasRole('ADMIN')")
public class GastoEventoController { private final GastoEventoService gastoService;

    public GastoEventoController(GastoEventoService gastoService) {
        this.gastoService = gastoService;
    }

    @PostMapping
    public ResponseEntity<GastoEventoResponseDTO> crear(
            @Valid @RequestBody GastoEventoRequestDTO dto) {
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

    // PROYECTADO → DESEMBOLSADO
    @PatchMapping("/{id}/desembolsar")
    public ResponseEntity<GastoEventoResponseDTO> desembolsar(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.desembolsar(id));
    }

    // Solo se puede eliminar si está PROYECTADO
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gastoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
