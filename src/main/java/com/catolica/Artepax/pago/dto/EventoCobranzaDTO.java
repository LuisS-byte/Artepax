package com.catolica.Artepax.pago.dto;

import java.math.BigDecimal;

/**
 * DTO representing an event's collection (cobranza) summary row.
 * <p>
 * Used by the cobranza listing endpoint ({@code /api/pagos/eventos-cobranza})
 * to display per-event financial data including totals, balances, pending
 * refunds, and pending payment counts.
 *
 * @param eventoId                   event identifier
 * @param clienteNombre              client display name
 * @param estadoEventoNombre         current event state name
 * @param precioTotal                total event price
 * @param totalCobrado               total amount collected (confirmed payments)
 * @param saldoPendiente             remaining unpaid balance
 * @param cobroCompleto              {@code true} if the event is fully paid
 * @param tieneDevolucionPendiente   {@code true} if there are pending refunds
 * @param totalDevolucionPendiente   total amount pending for refund
 * @param cantidadPagosPendientes    number of payments still in PENDIENTE state
 */
public record EventoCobranzaDTO(
    Long eventoId,
    String clienteNombre,
    String estadoEventoNombre,
    BigDecimal precioTotal,
    BigDecimal totalCobrado,
    BigDecimal saldoPendiente,
    Boolean cobroCompleto,
    Boolean tieneDevolucionPendiente,
    BigDecimal totalDevolucionPendiente,
    Integer cantidadPagosPendientes
) {}
