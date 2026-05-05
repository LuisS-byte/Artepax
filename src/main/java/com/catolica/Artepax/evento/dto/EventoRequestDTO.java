package com.catolica.Artepax.evento.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de entrada para la creación de un nuevo evento en estado COTIZACION.
 */
@Data
public class EventoRequestDTO {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "La decoración es obligatoria")
    private Long decoracionId;

    @NotNull(message = "El tipo de evento es obligatorio")
    private Long tipoEventoId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;
    private String nota;

    /** Cargo adicional sobre el precio base (opcional). */
    private BigDecimal precioAdicional;

    /** Justificación obligatoria si {@code precioAdicional > 0}. */
    private String notaPrecio;
}
