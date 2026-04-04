package com.catolica.Artepax.cronograma.controller;

import com.catolica.Artepax.cronograma.dto.CronogramaRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUpdateDTO;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cronogramas")
@PreAuthorize("hasRole('ADMIN')")
public class CronogramaController {

    private final CronogramaService cronogramaService;

    public CronogramaController(CronogramaService cronogramaService) {
        this.cronogramaService = cronogramaService;
    }

    // Crear actividad de cronograma
    @PostMapping
    public ResponseEntity<CronogramaResponseDTO> crear(
            @Valid @RequestBody CronogramaRequestDTO requestDTO) {
        return ResponseEntity.status(201).body(cronogramaService.crear(requestDTO));
    }

    // Obtener cronograma completo de un evento
    @GetMapping("/evento/{idEvento}")
    public ResponseEntity<List<CronogramaResponseDTO>> obtenerPorEvento(
            @PathVariable Long idEvento) {
        return ResponseEntity.ok(cronogramaService.obtenerPorEvento(idEvento));
    }

    // Obtener actividad por ID
    @GetMapping("/{id}")
    public ResponseEntity<CronogramaResponseDTO> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(cronogramaService.obtenerPorId(id));
    }

    // Actualizar actividad
    @PutMapping("/{id}")
    public ResponseEntity<CronogramaResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody CronogramaUpdateDTO updateDTO) {
        return ResponseEntity.ok(cronogramaService.actualizar(id, updateDTO));
    }

    // Eliminar actividad
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cronogramaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
