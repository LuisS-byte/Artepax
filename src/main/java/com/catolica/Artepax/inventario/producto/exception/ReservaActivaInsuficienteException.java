package com.catolica.Artepax.inventario.producto.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an inventory adjustment would reduce physical stock below
 * the quantity currently reserved by active events.
 * Returns HTTP 409 Conflict.
 */
public class ReservaActivaInsuficienteException extends ArtepaxException {
    public ReservaActivaInsuficienteException(String nombreProducto, Integer cantidadFisica, Integer reservadaActiva) {
        super(HttpStatus.CONFLICT,
                "No se puede ajustar el stock de '" + nombreProducto + "' por debajo de la cantidad actualmente reservada. "
                        + "Físico: " + cantidadFisica + ", Reservado activo: " + reservadaActiva);
    }
}
