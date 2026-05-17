package com.catolica.Artepax.pago.controller;

import com.catolica.Artepax.pago.dto.EventoCobranzaDTO;
import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;
import com.catolica.Artepax.pago.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing payment (pago) CRUD operations.
 * <p>
 * All endpoints require {@code ADMIN} role. Provides payment registration,
 * confirmation, cancellation, per-event listing, payment summaries, and
 * the collection (cobranza) dashboard.
 */
@RestController
@RequestMapping("/api/pagos")
@PreAuthorize("hasRole('ADMIN')")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * Registers a new payment in PENDIENTE state for an event.
     *
     * @param dto the payment request body (eventoId, monto, metodoPagoId, conceptoPagoId)
     * @return 201 Created with the created payment in PENDIENTE state
     */
    @PostMapping
    public ResponseEntity<PagoResponseDTO> registrar(@Valid @RequestBody PagoRequestDTO dto) {
        return ResponseEntity.status(201).body(pagoService.registrarPago(dto));
    }

    /**
     * Confirms a pending payment, moving it to PAGADO state.
     *
     * @param id the payment ID to confirm
     * @return 200 OK with the confirmed payment
     */
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PagoResponseDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.confirmarPago(id));
    }

    /**
     * Cancels a pending payment, moving it to CANCELADO state.
     * Only payments in PENDIENTE state can be cancelled.
     *
     * @param id the payment ID to cancel
     * @return 200 OK with the cancelled payment
     */
    @PatchMapping("/{id}/anular")
    public ResponseEntity<PagoResponseDTO> anular(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.anularPago(id));
    }

    /**
     * Lists all payments for a given event.
     *
     * @param eventoId the event ID
     * @return 200 OK with the list of payments
     */
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<PagoResponseDTO>> porEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(pagoService.obtenerPorEvento(eventoId));
    }

    /**
     * Returns the payment summary for an event, including totals and advance info.
     *
     * @param eventoId the event ID
     * @return 200 OK with the payment summary
     */
    @GetMapping("/evento/{eventoId}/resumen")
    public ResponseEntity<ResumenPagoEventoDTO> getResumenPagos(@PathVariable Long eventoId) {
        return ResponseEntity.ok(pagoService.getResumenPagos(eventoId));
    }

    /**
     * Returns a paginated list of all payments in the system.
     *
     * @param pageable pagination and sorting (default: size=50, sort=id)
     * @return 200 OK with the page of payments
     */
    @GetMapping
    public ResponseEntity<Page<PagoResponseDTO>> obtenerTodos(
            @PageableDefault(size = 50, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagoService.obtenerTodos(pageable));
    }

    /**
     * Returns the collection (cobranza) dashboard listing active events
     * with their financial status. Supports optional filters for event state,
     * pending balances, refunds, pending payments, and text search.
     *
     * @param estado                  optional list of event states to filter by
     * @param soloConSaldo            if {@code true}, only events with outstanding balance
     * @param soloConDevolucion       if {@code true}, only events with pending refunds
     * @param soloConPagosPendientes  if {@code true}, only events with pending payments
     * @param q                       optional text search (by client name or event ID)
     * @param limit                   optional max results
     * @return 200 OK with the filtered and sorted cobranza list
     */
    @GetMapping("/eventos-cobranza")
    public ResponseEntity<List<EventoCobranzaDTO>> getCobranza(
            @RequestParam(required = false) List<String> estado,
            @RequestParam(required = false) Boolean soloConSaldo,
            @RequestParam(required = false) Boolean soloConDevolucion,
            @RequestParam(required = false) Boolean soloConPagosPendientes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit) {
        Map<String, Object> filtros = new HashMap<>();
        if (estado != null) filtros.put("estado", estado);
        if (soloConSaldo != null) filtros.put("soloConSaldo", soloConSaldo);
        if (soloConDevolucion != null) filtros.put("soloConDevolucion", soloConDevolucion);
        if (soloConPagosPendientes != null) filtros.put("soloConPagosPendientes", soloConPagosPendientes);
        if (q != null) filtros.put("q", q);
        if (limit != null) filtros.put("limit", limit);
        return ResponseEntity.ok(pagoService.getEventosCobranza(filtros));
    }
}