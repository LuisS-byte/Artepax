package com.catolica.Artepax.pago.dto;

import java.math.BigDecimal;

public record AnticipoInfoDTO(
    BigDecimal porcentajeAnticipoMinimo,
    BigDecimal montoMinimoAnticipo,
    BigDecimal anticipoConfirmado,
    BigDecimal anticipoPorConcepto,
    BigDecimal faltanteParaAnticipo,
    Boolean anticipoCumplido,
    Boolean puedeConfirmarsePorAnticipo
) {}
