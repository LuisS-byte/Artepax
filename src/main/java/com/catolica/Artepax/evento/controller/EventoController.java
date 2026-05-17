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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    /**
     * Crea un nuevo evento en estado COTIZACION.
     *
     * @param requestDTO datos de creación del evento
     * @return 201 con el evento creado
     */
    @PostMapping
    public ResponseEntity<EventoResponseDTO> crearEvento(
            @Valid @RequestBody EventoRequestDTO requestDTO) {
        log.info("POST /api/eventos clienteId={} decoracionId={}",
                requestDTO.getClienteId(), requestDTO.getDecoracionId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventoService.crearEvento(requestDTO));
    }

    /**
     * Retorna los eventos del sistema con paginación.
     * <p>
     * Por defecto página 0, tamaño 50, ordenados por fecha descendente.
     *
     * @param pageable parámetros de página, tamaño y orden
     * @return página de eventos
     */
    @GetMapping
    public ResponseEntity<Page<EventoResponseDTO>> obtenerEventos(
            @PageableDefault(size = 50, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(eventoService.obtenerEventos(pageable));
    }

    /**
     * Retorna un evento por su identificador.
     *
     * @param id identificador del evento
     * @return evento encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> obtenerEventoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerEventoPorId(id));
    }

    /**
     * Actualiza los datos de un evento.
     * <p>
     * En COTIZACION permite edición completa; en PROGRAMADO solo edición parcial.
     *
     * @param id        identificador del evento
     * @param updateDTO datos a actualizar
     * @return evento actualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> actualizarEvento(
            @PathVariable Long id,
            @Valid @RequestBody EventoUpdateDTO updateDTO) {
        log.info("PUT /api/eventos/{}", id);
        return ResponseEntity.ok(eventoService.actualizarEvento(id, updateDTO));
    }

    /**
     * Agrega productos extra a un evento en estado PROGRAMADO o CONFIRMADO.
     *
     * @param id         identificador del evento
     * @param requestDTO lista de productos extra, nota de precio y monto adicional
     * @return evento actualizado con los extras incorporados
     */
    @PatchMapping("/{id}/extras")
    public ResponseEntity<EventoResponseDTO> agregarExtras(
            @PathVariable Long id,
            @Valid @RequestBody EventoExtraRequestDTO requestDTO) {
        log.info("PATCH /api/eventos/{}/extras items={}", id, requestDTO.getItems().size());
        return ResponseEntity.ok(eventoService.agregarExtras(id, requestDTO));
    }

    /**
     * Transición COTIZACION → PROGRAMADO. Reserva stock de productos.
     *
     * @param id identificador del evento
     * @return evento programado
     */
    @PatchMapping("/{id}/programar")
    public ResponseEntity<EventoResponseDTO> programarEvento(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/programar", id);
        return ResponseEntity.ok(eventoService.programarEvento(id));
    }

    /**
     * Transición PROGRAMADO → CONFIRMADO. Valida anticipo mínimo.
     *
     * @param id identificador del evento
     * @return evento confirmado
     */
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<EventoResponseDTO> confirmarEvento(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/confirmar", id);
        return ResponseEntity.ok(eventoService.confirmarEvento(id));
    }

    /**
     * Transición CONFIRMADO → EN_MONTAJE.
     *
     * @param id identificador del evento
     * @return evento en montaje
     */
    @PatchMapping("/{id}/iniciar-montaje")
    public ResponseEntity<EventoResponseDTO> iniciarMontaje(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/iniciar-montaje", id);
        return ResponseEntity.ok(eventoService.iniciarMontaje(id));
    }

    /**
     * Transición EN_MONTAJE → FINALIZADO o PENDIENTE_COBRO.
     * Consume stock de inventario.
     *
     * @param id identificador del evento
     * @return evento finalizado
     */
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<EventoResponseDTO> finalizarEvento(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/finalizar", id);
        return ResponseEntity.ok(eventoService.finalizarEvento(id));
    }

    /**
     * Inicia proceso de cancelación.
     * <p>
     * En COTIZACION cancela directamente. En estados avanzados pasa a
     * CANCELACION_PENDIENTE con tiempo de gracia reversible.
     *
     * @param id   identificador del evento
     * @param body contiene el motivo de cancelación
     * @return evento con cancelación iniciada
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<EventoResponseDTO> iniciarCancelacion(
            @PathVariable Long id,
            @Valid @RequestBody CancelacionEventoRequestDTO body) {
        log.info("PATCH /api/eventos/{}/cancelar motivo={}", id, body.getMotivo());
        return ResponseEntity.ok(eventoService.iniciarCancelacion(id, body.getMotivo()));
    }

    /**
     * Revierte una cancelación pendiente, restaurando el estado anterior.
     *
     * @param id identificador del evento
     * @return evento con estado restaurado
     */
    @PatchMapping("/{id}/revertir-cancelacion")
    public ResponseEntity<EventoResponseDTO> revertirCancelacion(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/revertir-cancelacion", id);
        return ResponseEntity.ok(eventoService.revertirCancelacion(id));
    }

    /**
     * Confirma la cancelación definitiva: CANCELACION_PENDIENTE → CANCELADO.
     *
     * @param id identificador del evento
     * @return 204 sin contenido
     */
    @PatchMapping("/{id}/confirmar-cancelacion")
    public ResponseEntity<Void> confirmarCancelacion(@PathVariable Long id) {
        log.info("PATCH /api/eventos/{}/confirmar-cancelacion", id);
        eventoService.confirmarCancelacion(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifica si existe disponibilidad en una fecha y horario.
     *
     * @param fecha           fecha a verificar
     * @param horaInicio      hora de inicio del bloque
     * @param horaFin         hora de fin del bloque
     * @param ubicacion       ubicación del evento (validada en service, no filtra en BD)
     * @param eventoIdExcluir evento a excluir de la verificación (opcional)
     * @return true si hay disponibilidad, false si hay solapamiento
     */
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

    /**
     * Retorna los eventos dentro de un rango de fechas para el calendario.
     *
     * @param inicio fecha de inicio del rango
     * @param fin    fecha de fin del rango
     * @return lista de eventos en el período
     */
    @GetMapping("/calendario")
    public ResponseEntity<List<CalendarioEventoDTO>> obtenerCalendario(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fin) {
        return ResponseEntity.ok(eventoService.obtenerCalendario(inicio, fin));
    }

    /**
     * Retorna la rentabilidad de un evento.
     *
     * @param id identificador del evento
     * @return datos de rentabilidad
     */
    @GetMapping("/{id}/rentabilidad")
    public ResponseEntity<RentabilidadEventoDTO> getRentabilidad(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.getRentabilidad(id));
    }

    /**
     * Retorna un resumen interno completo de un evento para uso administrativo.
     *
     * @param id identificador del evento
     * @return resumen con pagos, gastos y rentabilidad
     */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<EventoResumenInternoDTO> getDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.getResumenInterno(id));
    }

    /**
     * Retorna los productos asociados a un evento.
     * <p>
     * En COTIZACION devuelve la composición de la decoración; en adelante las reservas efectivas.
     *
     * @param id identificador del evento
     * @return lista de productos con cantidad, costo y origen
     */
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<EventoProductoDTO>> obtenerProductos(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtenerProductosPorEvento(id));
    }
}
