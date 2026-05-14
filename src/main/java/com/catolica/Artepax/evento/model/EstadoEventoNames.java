package com.catolica.Artepax.evento.model;

import java.util.Set;

/**
 * Constantes de nombres de estados del evento.
 * <p>
 * Define los valores válidos del catálogo {@code estado_evento} y conjuntos
 * utilitarios para validaciones de transiciones y consultas.
 */
public final class EstadoEventoNames {

    private EstadoEventoNames() {
    }

    /** Estado inicial: propuesta sin compromiso de stock. */
    public static final String COTIZACION = "COTIZACION";

    /** Stock reservado, evento agendado. */
    public static final String PROGRAMADO = "PROGRAMADO";

    /** Anticipo mínimo alcanzado, evento confirmado. */
    public static final String CONFIRMADO = "CONFIRMADO";

    /** Fase activa de preparación del evento. */
    public static final String EN_MONTAJE = "EN_MONTAJE";

    /** Evento completado, stock consumido y pago completo. */
    public static final String FINALIZADO = "FINALIZADO";

    /** Evento completado operativamente pero con pagos pendientes de cobro. */
    public static final String PENDIENTE_COBRO = "PENDIENTE_COBRO";

    /** Evento cancelado definitivamente. */
    public static final String CANCELADO = "CANCELADO";

    /** Estado intermedio con tiempo de gracia para revertir. */
    public static final String CANCELACION_PENDIENTE = "CANCELACION_PENDIENTE";

    /**
     * Estados que mantienen reservas activas en {@code evento_producto}.
     * Usado por {@code vista_stock_producto} para calcular disponibilidad.
     */
    public static final Set<String> ESTADOS_ACTIVOS_RESERVA = Set.of(
            PROGRAMADO,
            CONFIRMADO,
            EN_MONTAJE,
            CANCELACION_PENDIENTE
    );

    /** Estados desde los cuales se permite iniciar una cancelación con tiempo de gracia. */
    public static final Set<String> ESTADOS_PERMITIDOS_CANCELACION = Set.of(
            PROGRAMADO,
            CONFIRMADO,
            EN_MONTAJE
    );

    /** Estados en los cuales se permiten registrar gastos de evento. */
    public static final Set<String> ESTADOS_PERMITIDOS_GASTOS = Set.of(
            PROGRAMADO,
            CONFIRMADO,
            EN_MONTAJE
    );
}
