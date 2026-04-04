package com.catolica.Artepax.inventario.producto.exception;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String nombre, Integer disponible, Integer solicitado) {
        super("Stock insuficiente para '" + nombre + "'. Disponible: " + disponible + ", Solicitado: " + solicitado);
    }
}
