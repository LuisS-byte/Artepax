package com.catolica.Artepax.evento.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de entrada para la actualización de un evento.
 * <p>
 * Todos los campos son opcionales: solo se actualizan los valores no nulos.
 * En estado COTIZACION se permite cambiar decoracion, tipoEvento y datos de agenda.
 * En estado PROGRAMADO solo se permiten cambios de agenda, nota y notaPrecio.
 * clienteId siempre es inmutable.
 */
@Data
public class EventoUpdateDTO {
    private Long clienteId;
    private Long decoracionId;
    private Long tipoEventoId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;
    private String nota;
    private String notaPrecio;
}
