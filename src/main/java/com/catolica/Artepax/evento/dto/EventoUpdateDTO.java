package com.catolica.Artepax.evento.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de entrada para la actualización de un evento.
 * <p>
 * Todos los campos son opcionales: solo se actualizan los valores no nulos.
 * En estado COTIZACION se permite edición completa de todos los campos;
 * en estado PROGRAMADO solo edición parcial (sin cambio de cliente).
 */
@Data
public class EventoUpdateDTO {
    private Long clienteId;
    private Long decoracionId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;
    private String nota;
    private String notaPrecio;
}