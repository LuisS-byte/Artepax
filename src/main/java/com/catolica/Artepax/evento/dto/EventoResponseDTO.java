package com.catolica.Artepax.evento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO de salida para la representación completa de un evento en respuestas API.
 * Incluye datos denormalizados del cliente, decoración, estado y tipo de evento.
 */
@Data
public class EventoResponseDTO {

    private Long id;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;
    private String nota;
    private String motivoCancelacion;
    private LocalDateTime fechaCreacion;

    private Long clienteId;
    private String clienteNombre;

    private Long decoracionId;
    private String decoracionNombre;

    private Long estadoEventoId;
    private String estadoEventoNombre;

    private Long tipoEventoId;
    private String tipoEventoNombre;

    private BigDecimal precioBase;
    private BigDecimal precioAdicional;
    private BigDecimal precioTotal;
    private String notaPrecio;

    private Boolean clienteNotificado;
    private LocalDateTime fechaConfirmacion;
    private LocalDateTime fechaCancelacion;
    private LocalDate fechaExpiracion;
}
