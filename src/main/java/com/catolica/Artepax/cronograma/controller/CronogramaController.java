package com.catolica.Artepax.cronograma.controller;

import com.catolica.Artepax.cronograma.dto.CronogramaEstadoRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUpdateDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUsuarioRequestDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUsuarioResponseDTO;
import com.catolica.Artepax.cronograma.service.CronogramaService;
import jakarta.validation.Valid;
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
 * REST controller para gestión de cronogramas de eventos.
 * Todas las rutas requieren rol ADMIN.
 */
@RestController
@RequestMapping("/api/cronogramas")
@PreAuthorize("hasRole('ADMIN')")
public class CronogramaController {

    private static final Logger log = LoggerFactory.getLogger(CronogramaController.class);

    private final CronogramaService cronogramaService;

    public CronogramaController(CronogramaService cronogramaService) {
        this.cronogramaService = cronogramaService;
    }

    @PostMapping
    public ResponseEntity<CronogramaResponseDTO> crear(
            @Valid @RequestBody CronogramaRequestDTO dto) {
        log.info("Creando actividad de cronograma para evento {}", dto.getIdEvento());
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
            @Valid @RequestBody CronogramaUpdateDTO dto) {
        log.info("Actualizando actividad de cronograma {}", id);
        return ResponseEntity.ok(cronogramaService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<CronogramaResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CronogramaEstadoRequestDTO dto) {
        log.info("Cambiando estado de actividad {}: {}", id, dto.getEstado());
        return ResponseEntity.ok(cronogramaService.cambiarEstado(id, dto.getEstado()));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        log.info("Cancelando actividad de cronograma {}", id);
        cronogramaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/usuarios")
    public ResponseEntity<CronogramaUsuarioResponseDTO> asignarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody CronogramaUsuarioRequestDTO dto) {
        log.info("Asignando usuario a actividad {}", id);
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
        log.info("Removiendo usuario {} de actividad {}", usuarioId, id);
        cronogramaService.removerUsuario(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<CronogramaResponseDTO>> obtenerTodos(
            @PageableDefault(size = 50, sort = "fechaInicio") Pageable pageable) {
        return ResponseEntity.ok(cronogramaService.obtenerTodos(pageable));
    }
}
