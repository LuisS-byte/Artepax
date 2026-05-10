package com.catolica.Artepax.evento.dto;

import java.math.BigDecimal;

public record ResumenGastoEventoDTO(
        BigDecimal totalProyectado,
        BigDecimal totalDesembolsado,
        BigDecimal totalPendiente
) {}
