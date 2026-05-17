package com.catolica.Artepax.evento.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO de entrada para el motivo de cancelación de un evento.
 */
@Data
public class CancelacionEventoRequestDTO {

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String motivo;
}
