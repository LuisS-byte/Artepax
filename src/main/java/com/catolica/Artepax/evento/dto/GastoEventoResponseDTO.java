package com.catolica.Artepax.evento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida para la representación de un gasto de evento.
 * Incluye datos denormalizados del cliente y estado del evento.
 */
@Data
public class GastoEventoResponseDTO {

    private Long id;
    private Long eventoId;
    private String gastoConcepto;
    private BigDecimal gastoMonto;
    private String gastoEstado;
    private LocalDateTime gastoFecha;
    private String gastoNota;
    private String clienteNombre;
    private String estadoEventoNombre;
}
