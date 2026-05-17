package com.catolica.Artepax.pago.dto;

import java.math.BigDecimal;

/**
 * DTO carrying advance-payment (anticipo) information for an event's payment summary.
 * <p>
 * Provides the minimum required advance amount, how much has been confirmed,
 * whether the threshold has been met, and whether the event can transition
 * from {@code PROGRAMADO} to {@code CONFIRMADO} based on the advance.
 *
 * @param porcentajeAnticipoMinimo  minimum percentage of total price required as advance
 * @param montoMinimoAnticipo       minimum amount required as advance (in currency units)
 * @param anticipoConfirmado        total amount confirmed as payment towards the event
 * @param anticipoPorConcepto       amount confirmed specifically under the ANTICIPO concept
 * @param faltanteParaAnticipo      remaining amount needed to meet the advance threshold
 * @param anticipoCumplido          {@code true} if the confirmed amount meets or exceeds the minimum
 * @param puedeConfirmarsePorAnticipo  {@code true} if the event is in PROGRAMADO state and anticipo is met
 */
public record AnticipoInfoDTO(
    BigDecimal porcentajeAnticipoMinimo,
    BigDecimal montoMinimoAnticipo,
    BigDecimal anticipoConfirmado,
    BigDecimal anticipoPorConcepto,
    BigDecimal faltanteParaAnticipo,
    Boolean anticipoCumplido,
    Boolean puedeConfirmarsePorAnticipo
) {}
