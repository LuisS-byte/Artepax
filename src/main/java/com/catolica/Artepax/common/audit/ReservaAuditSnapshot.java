package com.catolica.Artepax.common.audit;

public record ReservaAuditSnapshot(
        Long idEvento,
        String estadoReserva,
        Integer cantidadProductosAfectados,
        String motivo
) {}
