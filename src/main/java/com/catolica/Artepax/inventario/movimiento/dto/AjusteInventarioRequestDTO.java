package com.catolica.Artepax.inventario.movimiento.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AjusteInventarioRequestDTO(
        @NotNull(message = "La cantidad física es obligatoria")
        @Min(value = 0, message = "La cantidad física no puede ser negativa")
        Integer cantidadFisica,

        @NotBlank(message = "El motivo es obligatorio")
        String motivo
) {}
