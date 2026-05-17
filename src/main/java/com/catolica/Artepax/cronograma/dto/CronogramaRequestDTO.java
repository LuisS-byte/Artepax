package com.catolica.Artepax.cronograma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO para crear una nueva actividad en el cronograma de un evento.
 * Las fechas deben estar dentro del rango del evento y el evento debe estar
 * en estado PROGRAMADO, CONFIRMADO o EN_MONTAJE.
 */
public class CronogramaRequestDTO {

    @NotNull(message = "El id del evento es obligatorio")
    private Long idEvento;

    @NotBlank(message = "La actividad es obligatoria")
    private String actividad;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fechaFin;

    public Long getIdEvento() { return idEvento; }
    public void setIdEvento(Long idEvento) { this.idEvento = idEvento; }

    public String getActividad() { return actividad; }
    public void setActividad(String actividad) { this.actividad = actividad; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
}
