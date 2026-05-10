package com.catolica.Artepax.cronograma.exception;

/**
 * Códigos de negocio del módulo cronograma para consumo del frontend.
 */
public final class CronogramaErrorCode {

    public static final String TRANSICION_INVALIDA = "CRONOGRAMA_TRANSICION_INVALIDA";
    public static final String NO_EDITABLE = "CRONOGRAMA_NO_EDITABLE";
    public static final String FECHA_INVALIDA = "CRONOGRAMA_FECHA_INVALIDA";
    public static final String FUERA_RANGO_EVENTO = "CRONOGRAMA_FUERA_RANGO_EVENTO";
    public static final String EVENTO_NO_PERMITE_EDICION = "CRONOGRAMA_EVENTO_NO_PERMITE_EDICION";

    private CronogramaErrorCode() {
    }
}
