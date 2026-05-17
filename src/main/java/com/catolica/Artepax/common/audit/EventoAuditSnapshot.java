package com.catolica.Artepax.common.audit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Snapshot de un evento para registros de auditoría.
 * Captura los campos relevantes del evento (estado, precios, fechas, ubicación)
 * antes o después de una mutación.
 */
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
