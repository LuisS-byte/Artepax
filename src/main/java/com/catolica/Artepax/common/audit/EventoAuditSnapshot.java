package com.catolica.Artepax.common.audit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventoAuditSnapshot(
        Long idEvento,
        String estado,
        BigDecimal precioBase,
        BigDecimal precioAdicional,
        BigDecimal precioTotal,
        LocalDate fechaEvento,
        LocalTime horaInicio,
        LocalTime horaFin,
        String ubicacion,
        String motivoCancelacion
) {}
