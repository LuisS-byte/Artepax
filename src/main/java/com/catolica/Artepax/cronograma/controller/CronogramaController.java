package com.catolica.Artepax.cronograma.controller;

import com.catolica.Artepax.cronograma.dto.*;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cronogramas")
@PreAuthorize("hasRole('ADMIN')")
public class CronogramaController {private final CronogramaService cronogramaService;

    public CronogramaController(CronogramaService cronogramaService) {
        this.cronogramaService = cronogramaService;
    }

    // ─── CRUD ACTIVIDADES ─────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CronogramaResponseDTO> crear(
            @Valid @RequestBody CronogramaRequestDTO dto) {
        return ResponseEntity.status(201).body(cronogramaService.crear(dto));
    }

    @GetMapping("/evento/{idEvento}")
    public ResponseEntity<List<CronogramaResponseDTO>> obtenerPorEvento(
            @PathVariable Long idEvento) {
        return ResponseEntity.ok(cronogramaService.obtenerPorEvento(idEvento));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CronogramaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cronogramaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CronogramaResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody CronogramaUpdateDTO dto) {
        return ResponseEntity.ok(cronogramaService.actualizar(id, dto));
    }

    // ─── ESTADOS ──────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CronogramaResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        return ResponseEntity.ok(cronogramaService.cambiarEstado(id, nuevoEstado));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        cronogramaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    // ─── USUARIOS ASIGNADOS ───────────────────────────────────────────────────

    @PostMapping("/{id}/usuarios")
    public ResponseEntity<CronogramaUsuarioResponseDTO> asignarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody CronogramaUsuarioRequestDTO dto) {
        return ResponseEntity.status(201).body(
                cronogramaService.asignarUsuario(id, dto));
    }

    @GetMapping("/{id}/usuarios")
    public ResponseEntity<List<CronogramaUsuarioResponseDTO>> obtenerUsuarios(
            @PathVariable Long id) {
        return ResponseEntity.ok(cronogramaService.obtenerUsuarios(id));
    }

    @DeleteMapping("/{id}/usuarios/{usuarioId}")
    public ResponseEntity<Void> removerUsuario(
            @PathVariable Long id,
            @PathVariable Long usuarioId) {
        cronogramaService.removerUsuario(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}