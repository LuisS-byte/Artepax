package com.catolica.Artepax.cronograma.exception;

/**
 * Códigos de negocio del módulo cronograma para consumo del frontend.
 * Cada constante representa un error de negocio identificable por el cliente.
 */
public final class CronogramaErrorCode {

    /** Transición de estado no permitida. */
    public static final String TRANSICION_INVALIDA = "CRONOGRAMA_TRANSICION_INVALIDA";

    /** Intento de editar una actividad en estado terminal. */
    public static final String NO_EDITABLE = "CRONOGRAMA_NO_EDITABLE";

    /** Fecha de inicio posterior o igual a la fecha de fin. */
    public static final String FECHA_INVALIDA = "CRONOGRAMA_FECHA_INVALIDA";

    /** Fecha de la actividad fuera del rango del evento. */
    public static final String FUERA_RANGO_EVENTO = "CRONOGRAMA_FUERA_RANGO_EVENTO";

    /** El evento no está en un estado que permita modificar su cronograma. */
    public static final String EVENTO_NO_PERMITE_EDICION = "CRONOGRAMA_EVENTO_NO_PERMITE_EDICION";

    private CronogramaErrorCode() {
    }
}
