package com.catolica.Artepax.cronograma.entity;

/**
 * Constantes de estados operativos del cronograma.
 * Flujo: PENDIENTE → EN_PROGRESO → COMPLETADO | CANCELADO.
 */
public final class CronogramaEstados {

    /** Actividad pendiente de iniciar. Estado inicial por defecto. */
    public static final String PENDIENTE = "PENDIENTE";

    /** Actividad en curso. */
    public static final String EN_PROGRESO = "EN_PROGRESO";

    /** Actividad finalizada exitosamente. */
    public static final String COMPLETADO = "COMPLETADO";

    /** Actividad cancelada. Estado terminal junto con COMPLETADO. */
    public static final String CANCELADO = "CANCELADO";

    private CronogramaEstados() {
    }
}
