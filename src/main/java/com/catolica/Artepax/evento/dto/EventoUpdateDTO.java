package com.catolica.Artepax.evento.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventoUpdateDTO {
    private Long clienteId;
    private Long decoracionId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;
    private String nota;
}