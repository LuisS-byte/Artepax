package com.catolica.Artepax.evento.controller;

import com.catolica.Artepax.evento.dto.EventoGastoControlDTO;
import com.catolica.Artepax.evento.dto.GastoEventoRequestDTO;
import com.catolica.Artepax.evento.dto.GastoEventoResponseDTO;
import com.catolica.Artepax.evento.dto.ResumenGastoEventoDTO;
import com.catolica.Artepax.evento.service.GastoEventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /**
     * Crea un nuevo gasto en estado PROYECTADO para un evento.
     *
     * @param dto datos del gasto a crear
     * @return 201 con el gasto creado
     */
    @PostMapping
    public ResponseEntity<GastoEventoResponseDTO> crear(
            @Valid @RequestBody GastoEventoRequestDTO dto) {
        log.info("POST /api/gastos eventoId={}", dto.getEventoId());
        return ResponseEntity.status(201).body(gastoService.crear(dto));
    }

    /**
     * Retorna todos los gastos de un evento.
     *
     * @param eventoId identificador del evento
     * @return lista de gastos del evento
     */
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<GastoEventoResponseDTO>> obtenerPorEvento(
            @PathVariable Long eventoId) {
        return ResponseEntity.ok(gastoService.obtenerPorEvento(eventoId));
    }

    /**
     * Retorna el resumen de gastos de un evento (proyectado, desembolsado, pendiente).
     *
     * @param eventoId identificador del evento
     * @return resumen de gastos
     */
    @GetMapping("/evento/{eventoId}/resumen")
    public ResponseEntity<ResumenGastoEventoDTO> obtenerResumen(
            @PathVariable Long eventoId) {
        return ResponseEntity.ok(gastoService.getResumen(eventoId));
    }

    /**
     * Retorna la lista de control de gastos para todos los eventos,
     * incluyendo totales proyectados y desembolsados.
     *
     * @return lista de control de gastos por evento
     */
    @GetMapping("/eventos-control")
    public ResponseEntity<List<EventoGastoControlDTO>> obtenerEventosControl() {
        log.info("GET /api/gastos/eventos-control");
        return ResponseEntity.ok(gastoService.obtenerEventosControl());
    }

    /**
     * Retorna un gasto por su identificador.
     *
     * @param id identificador del gasto
     * @return gasto encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<GastoEventoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.obtenerPorId(id));
    }

    /**
     * Desembolsa un gasto: transición PROYECTADO → DESEMBOLSADO.
     *
     * @param id identificador del gasto
     * @return gasto desembolsado
     */
    @PatchMapping("/{id}/desembolsar")
    public ResponseEntity<GastoEventoResponseDTO> desembolsar(@PathVariable Long id) {
        log.info("PATCH /api/gastos/{}/desembolsar", id);
        return ResponseEntity.ok(gastoService.desembolsar(id));
    }

    /**
     * Elimina un gasto en estado PROYECTADO. No permite eliminar gastos desembolsados.
     *
     * @param id identificador del gasto
     * @return 204 sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/gastos/{}", id);
        gastoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna todos los gastos paginados.
     *
     * @param pageable parámetros de paginación y ordenamiento
     * @return página de gastos
     */
    @GetMapping
    public ResponseEntity<Page<GastoEventoResponseDTO>> obtenerTodos(
            @PageableDefault(size = 50, sort = "gastoFecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(gastoService.obtenerTodos(pageable));
    }
}
