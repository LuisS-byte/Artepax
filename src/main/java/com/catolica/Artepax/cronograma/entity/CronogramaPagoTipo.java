package com.catolica.Artepax.cronograma.entity;

import java.util.Set;

/**
 * Tipo de pago para la asignación de usuarios a actividades del cronograma.
 * FIJO: monto fijo por la actividad.
 * POR_HORA: pago basado en horas dedicadas.
 */
public enum CronogramaPagoTipo {

    /** Monto fijo por la actividad completa. */
    FIJO("FIJO"),
    /** Pago proporcional a las horas dedicadas. */
    POR_HORA("POR_HORA");

    private final String valor;

    CronogramaPagoTipo(String valor) {
        this.valor = valor;
    }

    /**
     * Retorna el valor textual del tipo de pago.
     *
     * @return "FIJO" o "POR_HORA"
     */
    public String getValor() {
        return valor;
    }

    /** Conjunto inmutable con todos los valores válidos. */
    public static final Set<String> VALORES = Set.of(FIJO.valor, POR_HORA.valor);
}
