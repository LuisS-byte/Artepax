package com.catolica.Artepax.inventario.movimiento.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for an inventory adjustment request based on physical count.
 * Carries the observed physical quantity and a required reason for the adjustment.
 */
public record AjusteInventarioRequestDTO(
        @NotNull(message = "La cantidad física es obligatoria")
        @Min(value = 0, message = "La cantidad física no puede ser negativa")
        Integer cantidadFisica,

        @NotBlank(message = "El motivo es obligatorio")
        String motivo
) {}
