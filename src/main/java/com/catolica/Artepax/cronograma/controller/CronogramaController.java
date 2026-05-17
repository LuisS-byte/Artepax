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

    /**
     * Crea una nueva actividad de cronograma para un evento.
     *
     * @param dto datos de la actividad a crear
     * @return la actividad creada con estado 201
     */
    @PostMapping
    public ResponseEntity<CronogramaResponseDTO> crear(
            @Valid @RequestBody CronogramaRequestDTO dto) {
        log.info("Creando actividad de cronograma para evento {}", dto.getIdEvento());
        return ResponseEntity.status(201).body(cronogramaService.crear(dto));
    }

    /**
     * Obtiene todas las actividades de cronograma de un evento.
     *
     * @param idEvento identificador del evento
     * @return lista de actividades del cronograma
     */
    @GetMapping("/evento/{idEvento}")
    public ResponseEntity<List<CronogramaResponseDTO>> obtenerPorEvento(
            @PathVariable Long idEvento) {
        return ResponseEntity.ok(cronogramaService.obtenerPorEvento(idEvento));
    }

    /**
     * Obtiene una actividad de cronograma por su identificador.
     *
     * @param id identificador de la actividad
     * @return la actividad encontrada
     */
    @GetMapping("/{id}")
    public ResponseEntity<CronogramaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cronogramaService.obtenerPorId(id));
    }

    /**
     * Actualiza parcialmente una actividad de cronograma.
     *
     * @param id  identificador de la actividad
     * @param dto datos a actualizar (campos no nulos)
     * @return la actividad actualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<CronogramaResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CronogramaUpdateDTO dto) {
        log.info("Actualizando actividad de cronograma {}", id);
        return ResponseEntity.ok(cronogramaService.actualizar(id, dto));
    }

    /**
     * Cambia el estado de una actividad de cronograma.
     *
     * @param id  identificador de la actividad
     * @param dto contiene el nuevo estado
     * @return la actividad con el estado actualizado
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<CronogramaResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CronogramaEstadoRequestDTO dto) {
        log.info("Cambiando estado de actividad {}: {}", id, dto.getEstado());
        return ResponseEntity.ok(cronogramaService.cambiarEstado(id, dto.getEstado()));
    }

    /**
     * Cancela una actividad de cronograma.
     *
     * @param id identificador de la actividad a cancelar
     * @return 204 sin contenido
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        log.info("Cancelando actividad de cronograma {}", id);
        cronogramaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Asigna un usuario a una actividad de cronograma.
     *
     * @param id  identificador de la actividad
     * @param dto datos de asignación (usuario, pago, tipo de pago)
     * @return la asignación creada con estado 201
     */
    @PostMapping("/{id}/usuarios")
    public ResponseEntity<CronogramaUsuarioResponseDTO> asignarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody CronogramaUsuarioRequestDTO dto) {
        log.info("Asignando usuario a actividad {}", id);
        return ResponseEntity.status(201).body(
                cronogramaService.asignarUsuario(id, dto));
    }

    /**
     * Obtiene los usuarios asignados a una actividad de cronograma.
     *
     * @param id identificador de la actividad
     * @return lista de asignaciones de usuarios
     */
    @GetMapping("/{id}/usuarios")
    public ResponseEntity<List<CronogramaUsuarioResponseDTO>> obtenerUsuarios(
            @PathVariable Long id) {
        return ResponseEntity.ok(cronogramaService.obtenerUsuarios(id));
    }

    /**
     * Remueve un usuario de una actividad de cronograma.
     *
     * @param id        identificador de la actividad
     * @param usuarioId identificador del usuario a remover
     * @return 204 sin contenido
     */
    @DeleteMapping("/{id}/usuarios/{usuarioId}")
    public ResponseEntity<Void> removerUsuario(
            @PathVariable Long id,
            @PathVariable Long usuarioId) {
        log.info("Removiendo usuario {} de actividad {}", usuarioId, id);
        cronogramaService.removerUsuario(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene todas las actividades de cronograma paginadas.
     *
     * @param pageable parámetros de paginación (por defecto 50 elementos, ordenado por fechaInicio)
     * @return página de actividades
     */
    @GetMapping
    public ResponseEntity<Page<CronogramaResponseDTO>> obtenerTodos(
            @PageableDefault(size = 50, sort = "fechaInicio") Pageable pageable) {
        return ResponseEntity.ok(cronogramaService.obtenerTodos(pageable));
    }
}
