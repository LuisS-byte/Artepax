package com.catolica.Artepax.common.audit;

/**
 * Snapshot de una reserva de producto para registros de auditoría.
 * Captura el estado de la reserva (evento, estado, cantidad) antes o después de una mutación.
 */
public record ReservaAuditSnapshot(
        Long idEvento,
        String estadoReserva,
        Integer cantidadProductosAfectados,
        String motivo
) {}
