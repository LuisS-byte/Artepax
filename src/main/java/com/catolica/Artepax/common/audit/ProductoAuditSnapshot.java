package com.catolica.Artepax.common.audit;

/**
 * Snapshot de un producto para registros de auditoría.
 * Captura el estado del producto (nombre, cantidad, estado) antes o después de una mutación.
 */
public record ProductoAuditSnapshot(
        Long idProducto,
        String nombre,
        Integer cantidadTotal,
        String estado
) {}
