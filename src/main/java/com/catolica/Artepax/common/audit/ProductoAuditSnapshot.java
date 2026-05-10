package com.catolica.Artepax.common.audit;

public record ProductoAuditSnapshot(
        Long idProducto,
        String nombre,
        Integer cantidadTotal,
        String estado
) {}
