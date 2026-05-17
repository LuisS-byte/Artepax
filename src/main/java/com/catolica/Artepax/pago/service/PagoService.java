package com.catolica.Artepax.pago.service;

import com.catolica.Artepax.pago.dto.EventoCobranzaDTO;
import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for payment (pago) operations.
 * <p>
 * Coordinates payment lifecycle: registration, confirmation, and cancellation,
 * as well as event state transitions triggered by payments:
 * <ul>
 *   <li>{@link #confirmarPago(Long)} may trigger PROGRAMADO → CONFIRMADO
 *       via {@code verificarAnticipo} when the advance threshold is met</li>
 *   <li>{@link #confirmarPago(Long)} may trigger PENDIENTE_COBRO → FINALIZADO
 *       via {@code finalizarDesdeCobroCompleto} when fully paid</li>
 * </ul>
 */
public interface PagoService {

    /**
     * Registers a new payment in PENDIENTE state for the given event.
     * Validates that the event is in an allowed state and that the total
     * amount does not exceed the event's remaining balance.
     *
     * @param dto the payment request data (event, amount, method, concept)
     * @return the created payment response with PENDIENTE state
     */
    PagoResponseDTO registrarPago(PagoRequestDTO dto);

    /**
     * Confirms a pending payment, moving it to PAGADO state.
     * <p>
     * After confirmation this method may trigger automatic event transitions:
     * <ul>
     *   <li>If the event is PROGRAMADO and the advance threshold is met,
     *       the event transitions to CONFIRMADO</li>
     *   <li>If the event becomes fully paid (PENDIENTE_COBRO), it transitions
     *       to FINALIZADO</li>
     * </ul>
     *
     * @param pagoId the ID of the payment to confirm
     * @return the confirmed payment response with updated event state info
     */
    PagoResponseDTO confirmarPago(Long pagoId);

    /**
     * Cancels a pending payment, moving it to CANCELADO state.
     * Only payments in PENDIENTE state can be cancelled.
     *
     * @param pagoId the ID of the payment to cancel
     * @return the cancelled payment response
     */
    PagoResponseDTO anularPago(Long pagoId);

    /**
     * Returns all payments for a given event, ordered naturally.
     *
     * @param eventoId the event ID
     * @return list of payment responses with fully resolved relations
     */
    List<PagoResponseDTO> obtenerPorEvento(Long eventoId);

    /**
     * Returns a paginated list of all payments in the system.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of payment responses
     */
    Page<PagoResponseDTO> obtenerTodos(Pageable pageable);

    /**
     * Returns a comprehensive payment summary for an event, including totals
     * from the {@code vista_pagos_evento} view and advance-payment information.
     *
     * @param eventoId the event ID
     * @return the payment summary DTO
     */
    ResumenPagoEventoDTO getResumenPagos(Long eventoId);

    /**
     * Returns a collection (cobranza) listing of active events with their
     * payment status, supporting filtering by state, balance, refunds,
     * pending payments, and text search.
     *
     * @param filtros filter map with optional keys: estado, soloConSaldo,
     *                soloConDevolucion, soloConPagosPendientes, q, limit
     * @return list of event cobranza DTOs sorted by priority
     */
    List<EventoCobranzaDTO> getEventosCobranza(Map<String, Object> filtros);
}
