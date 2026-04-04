package com.catolica.Artepax.cronograma.dto;

import java.time.LocalDateTime;

public class CronogramaUpdateDTO {

    private String actividad;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    public String getActividad() {
        return actividad;
    }

    public void setActividad(String actividad) {
        this.actividad = actividad;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }
}
