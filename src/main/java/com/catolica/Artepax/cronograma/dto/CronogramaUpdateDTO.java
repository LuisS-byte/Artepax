package com.catolica.Artepax.cronograma.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO para actualización parcial de una actividad del cronograma.
 * Solo los campos no nulos serán actualizados.
 */
public class CronogramaUpdateDTO {

    @Size(max = 255, message = "La actividad no puede exceder 255 caracteres")
    private String actividad;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    @Size(max = 255, message = "La observación no puede exceder 255 caracteres")
    private String cronoObservacion;

    public String getActividad() { return actividad; }
    public void setActividad(String actividad) { this.actividad = actividad; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public String getCronoObservacion() { return cronoObservacion; }
    public void setCronoObservacion(String cronoObservacion) { this.cronoObservacion = cronoObservacion; }
}
