package com.catolica.Artepax.cronograma.entity;

import java.util.Set;

/**
 * Tipo de pago para la asignación de usuarios a actividades del cronograma.
 * FIJO: monto fijo por la actividad.
 * POR_HORA: pago basado en horas dedicadas.
 */
public enum CronogramaPagoTipo {

    FIJO("FIJO"),
    POR_HORA("POR_HORA");

    private final String valor;

    CronogramaPagoTipo(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static final Set<String> VALORES = Set.of(FIJO.valor, POR_HORA.valor);
}
