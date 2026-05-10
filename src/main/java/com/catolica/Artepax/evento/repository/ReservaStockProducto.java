package com.catolica.Artepax.evento.repository;

/**
 * Proyección mínima para agrupar reservas activas por producto.
 */
public interface ReservaStockProducto {

    Long getProductoId();

    Integer getCantidadReservada();
}
