package com.catolica.Artepax.common.audit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoAuditSnapshot(
        Long idPago,
        Long idEvento,
        String estadoPago,
        String conceptoPago,
        String metodoPago,
        BigDecimal monto,
        LocalDateTime fecha
) {}
