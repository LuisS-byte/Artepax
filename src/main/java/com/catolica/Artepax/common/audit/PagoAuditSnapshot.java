package com.catolica.Artepax.common.audit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Snapshot de un pago para registros de auditoría.
 * Captura el estado del pago (monto, método, concepto) antes o después de una mutación.
 */
public record PagoAuditSnapshot(
        Long idPago,
        Long idEvento,
        String estadoPago,
        String conceptoPago,
        String metodoPago,
        BigDecimal monto,
        LocalDateTime fecha
) {}
