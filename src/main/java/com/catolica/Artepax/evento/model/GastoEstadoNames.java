package com.catolica.Artepax.evento.model;

/**
 * Constantes de nombres de estados de gasto de evento.
 * <p>
 * Define los dos valores válidos del ciclo de vida de un gasto:
 * {@link #PROYECTADO} (creado, pendiente de desembolso) y
 * {@link #DESEMBOLSADO} (efectivamente pagado).
 */
public final class GastoEstadoNames {

    private GastoEstadoNames() {
    }

    /** Estado inicial: gasto registrado pero no desembolsado. */
    public static final String PROYECTADO = "PROYECTADO";

    /** Estado final: gasto efectivamente pagado. */
    public static final String DESEMBOLSADO = "DESEMBOLSADO";
}
