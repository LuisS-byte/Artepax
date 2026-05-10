package com.catolica.Artepax.common.audit;

import java.time.LocalDateTime;

public record DocumentoAuditSnapshot(
        Long idDocumento,
        Long idEvento,
        String tipo,
        String estadoEnvio,
        Integer intentosEnvio,
        LocalDateTime fechaEnvio
) {}
