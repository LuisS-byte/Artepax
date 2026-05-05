package com.catolica.Artepax.cronograma.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitar cambio de estado de una actividad del cronograma.
 */
public class CronogramaEstadoRequestDTO {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
