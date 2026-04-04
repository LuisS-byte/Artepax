package com.catolica.Artepax.evento.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventoResponseDTO {
    private Long id;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String ubicacion;
    private Integer capacidad;
    private String descripcion;

    private Long clienteId;
    private String clienteNombre;

    private Long estadoEventoId;
}