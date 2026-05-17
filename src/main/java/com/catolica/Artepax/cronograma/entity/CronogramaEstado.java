package com.catolica.Artepax.cronograma.entity;

import java.util.Set;

/**
 * Estados válidos para actividades del cronograma.
 * Flujo: PENDIENTE → EN_PROGRESO → COMPLETADO (o CANCELADO en cualquier punto).
 */
public enum CronogramaEstado {

    PENDIENTE(CronogramaEstados.PENDIENTE),
    EN_PROGRESO(CronogramaEstados.EN_PROGRESO),
    COMPLETADO(CronogramaEstados.COMPLETADO),
    CANCELADO(CronogramaEstados.CANCELADO);

    private final String valor;

    CronogramaEstado(String valor) {
        this.valor = valor;
    }

    /**
     * Retorna el valor textual del estado.
     *
     * @return el nombre del estado (PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO)
     */
    public String getValor() {
        return valor;
    }

    /**
     * Estados del evento que permiten crear/editar actividades de cronograma.
     * La política real está en CronogramaPolicy. Este set es referencial.
     */
    public static final Set<String> ESTADOS_EVENTO_PERMITIDOS = Set.of("PROGRAMADO", "CONFIRMADO", "EN_MONTAJE");

    /**
     * Estados terminales que no permiten transiciones posteriores.
     */
    public static final Set<String> ESTADOS_FINALES = Set.of(CANCELADO.valor, COMPLETADO.valor);

    /**
     * Verifica si un string corresponde a un estado válido del cronograma.
     *
     * @param valor el string a validar
     * @return {@code true} si coincide con algún estado del enum
     */
    public static boolean isValid(String valor) {
        for (CronogramaEstado e : values()) {
            if (e.valor.equals(valor)) return true;
        }
        return false;
    }
}
