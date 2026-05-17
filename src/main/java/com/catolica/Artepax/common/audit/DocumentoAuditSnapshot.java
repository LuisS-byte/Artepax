package com.catolica.Artepax.common.audit;

import java.time.LocalDateTime;

/**
 * Snapshot de un documento para registros de auditoría.
 * Captura el estado del documento antes o después de una mutación.
 */
public record DocumentoAuditSnapshot(
        Long idDocumento,
        Long idEvento,
        String tipo,
        String estadoEnvio,
        Integer intentosEnvio,
        LocalDateTime fechaEnvio
) {}
