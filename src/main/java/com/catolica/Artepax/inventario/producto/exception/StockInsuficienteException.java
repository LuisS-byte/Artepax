package com.catolica.Artepax.inventario.producto.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class StockInsuficienteException extends ArtepaxException {
    public StockInsuficienteException(String nombre, Integer disponible, Integer solicitado) {
        super(HttpStatus.CONFLICT, "Stock insuficiente para '" + nombre + "'. Disponible: " + disponible + ", Solicitado: " + solicitado);
    }
}
