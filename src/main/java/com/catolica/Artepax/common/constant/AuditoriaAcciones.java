package com.catolica.Artepax.common.constant;

/**
 * Constantes con los nombres de acciones de auditoría.
 * Cada constante representa una operación que puede ser auditada en el sistema.
 * <p>
 * Las acciones se almacenan en la columna {@code accion} de la tabla {@code auditoria}.
 */
public final class AuditoriaAcciones {

    /** Creación de un nuevo registro. */
    public static final String CREAR = "CREAR";
    /** Edición o modificación de un registro existente. */
    public static final String EDITAR = "EDITAR";
    /** Programación de un evento. */
    public static final String PROGRAMAR = "PROGRAMAR";
    /** Confirmación de una operación (ej. pago). */
    public static final String CONFIRMAR = "CONFIRMAR";
    /** Inicio de montaje de un evento. */
    public static final String INICIAR_MONTAJE = "INICIAR_MONTAJE";
    /** Cancelación de un recurso (evento, reserva, etc.). */
    public static final String CANCELAR = "CANCELAR";
    /** Reversión de una cancelación previa. */
    public static final String REVERTIR_CANCELACION = "REVERTIR_CANCELACION";
    /** Finalización de un evento o proceso. */
    public static final String FINALIZAR = "FINALIZAR";
    /** Agregado de servicios o productos extra a un evento. */
    public static final String AGREGAR_EXTRAS = "AGREGAR_EXTRAS";
    /** Registro de un nuevo pago. */
    public static final String REGISTRAR_PAGO = "REGISTRAR_PAGO";
    /** Confirmación de un pago previamente registrado. */
    public static final String CONFIRMAR_PAGO = "CONFIRMAR_PAGO";
    /** Anulación de un pago existente. */
    public static final String ANULAR = "ANULAR";
    /** Ajuste manual de stock de un producto. */
    public static final String AJUSTAR_STOCK = "AJUSTAR_STOCK";
    /** Generación de un documento (factura, remito, etc.). */
    public static final String GENERAR_DOCUMENTO = "GENERAR_DOCUMENTO";
    /** Reenvío de un documento previamente generado. */
    public static final String REENVIAR_DOCUMENTO = "REENVIAR_DOCUMENTO";
    /** Creación de una reserva de producto para un evento. */
    public static final String CREAR_RESERVA = "CREAR_RESERVA";
    /** Liberación de una reserva de producto (sin consumo de stock). */
    public static final String LIBERAR_RESERVA = "LIBERAR_RESERVA";
    /** Consumo definitivo de una reserva (descarga de stock). */
    public static final String CONSUMIR_RESERVA = "CONSUMIR_RESERVA";
    /** Desembolso o egreso registrado. */
    public static final String DESEMBOLSAR = "DESEMBOLSAR";
    /** Eliminación lógica o física de un registro. */
    public static final String ELIMINAR = "ELIMINAR";

    private AuditoriaAcciones() {}
}
