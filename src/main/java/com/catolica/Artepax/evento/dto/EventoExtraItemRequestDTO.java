package com.catolica.Artepax.evento.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO que representa un producto individual dentro de una solicitud de extras.
 * <p>
 * Cada ítem contiene el identificador del producto y la cantidad a agregar.
 */
@Data
public class EventoExtraItemRequestDTO {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;
}
