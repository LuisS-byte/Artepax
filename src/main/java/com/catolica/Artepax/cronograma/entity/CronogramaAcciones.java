package com.catolica.Artepax.cronograma.entity;

/**
 * Acciones de UI/API derivadas del estado actual del cronograma.
 */
public final class CronogramaAcciones {

    /** Iniciar la actividad (PENDIENTE → EN_PROGRESO). */
    public static final String INICIAR = "INICIAR";

    /** Completar la actividad (EN_PROGRESO → COMPLETADO). */
    public static final String COMPLETAR = "COMPLETAR";

    /** Cancelar la actividad (PENDIENTE/EN_PROGRESO → CANCELADO). */
    public static final String CANCELAR = "CANCELAR";

    private CronogramaAcciones() {
    }
}
