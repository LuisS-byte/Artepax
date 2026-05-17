package com.catolica.Artepax.common.constant;

/**
 * Constantes con los nombres de tablas/entidades auditadas en el sistema.
 * Cada constante identifica la tabla afectada en un registro de auditoría.
 * <p>
 * Los valores se almacenan en la columna {@code tabla_afectada} de la tabla {@code auditoria}.
 */
public final class AuditoriaTablas {

    /** Tabla de eventos. */
    public static final String EVENTO = "evento";
    /** Tabla de pagos. */
    public static final String PAGO = "pago";
    /** Tabla de productos del inventario. */
    public static final String PRODUCTO = "producto";
    /** Tabla de movimientos de inventario. */
    public static final String MOVIMIENTO_INVENTARIO = "movimiento_inventario";
    /** Tabla de relación evento-producto (reservas). */
    public static final String EVENTO_PRODUCTO = "evento_producto";
    /** Tabla de documentos generados. */
    public static final String DOCUMENTO = "documento";
    /** Tabla de clientes. */
    public static final String CLIENTE = "cliente";
    /** Tabla de gastos asociados a eventos. */
    public static final String GASTO_EVENTO = "gasto_evento";

    private AuditoriaTablas() {}
}
