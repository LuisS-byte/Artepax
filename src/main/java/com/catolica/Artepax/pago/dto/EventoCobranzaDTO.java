package com.catolica.Artepax.pago.dto;

import java.math.BigDecimal;

public record EventoCobranzaDTO(
    Long eventoId,
    String clienteNombre,
    String estadoEventoNombre,
    BigDecimal precioTotal,
    BigDecimal totalCobrado,
    BigDecimal saldoPendiente,
    Boolean cobroCompleto,
    Boolean tieneDevolucionPendiente,
    BigDecimal totalDevolucionPendiente,
    Integer cantidadPagosPendientes
) {}
