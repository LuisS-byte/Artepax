package com.catolica.Artepax.evento.dto;

import java.math.BigDecimal;

/**
 * DTO de resumen de gastos de un evento.
 *
 * @param totalProyectado   suma de gastos en estado PROYECTADO
 * @param totalDesembolsado suma de gastos en estado DESEMBOLSADO
 * @param totalPendiente    monto total pendiente (equivale a totalProyectado)
 */
public record ResumenGastoEventoDTO(
        BigDecimal totalProyectado,
        BigDecimal totalDesembolsado,
        BigDecimal totalPendiente
) {}
