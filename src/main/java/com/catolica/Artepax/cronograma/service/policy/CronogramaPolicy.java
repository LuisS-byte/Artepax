package com.catolica.Artepax.cronograma.service.policy;

import com.catolica.Artepax.cronograma.entity.CronogramaAcciones;
import com.catolica.Artepax.cronograma.entity.CronogramaEstado;
import com.catolica.Artepax.cronograma.entity.CronogramaEstados;
import com.catolica.Artepax.cronograma.exception.CronogramaErrorCode;
import com.catolica.Artepax.cronograma.exception.CronogramaException;
import com.catolica.Artepax.evento.model.EstadoEventoNames;
import com.catolica.Artepax.evento.model.Evento;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Política de negocio para transiciones y edición del cronograma.
 */
@Component
public class CronogramaPolicy {

    private static final Set<String> ESTADOS_EVENTO_PERMITIDOS = Set.of(
            EstadoEventoNames.PROGRAMADO,
            EstadoEventoNames.CONFIRMADO,
            EstadoEventoNames.EN_MONTAJE
    );

    /**
     * Valida que la transición de estado sea permitida según la máquina de estados del cronograma.
     * <ul>
     *   <li>PENDIENTE → EN_PROGRESO | CANCELADO</li>
     *   <li>EN_PROGRESO → COMPLETADO | CANCELADO</li>
     *   <li>COMPLETADO / CANCELADO → ninguna (estados terminales)</li>
     * </ul>
     *
     * @param estadoActual estado actual de la actividad
     * @param estadoNuevo  estado destino deseado
     * @throws CronogramaException si la transición no está permitida
     */
    public void validarTransicion(String estadoActual, String estadoNuevo) {
        if (estadoActual.equals(estadoNuevo)) {
            return;
        }

        boolean permitida = switch (estadoActual) {
            case CronogramaEstados.PENDIENTE -> CronogramaEstados.EN_PROGRESO.equals(estadoNuevo)
                    || CronogramaEstados.CANCELADO.equals(estadoNuevo);
            case CronogramaEstados.EN_PROGRESO -> CronogramaEstados.COMPLETADO.equals(estadoNuevo)
                    || CronogramaEstados.CANCELADO.equals(estadoNuevo);
            case CronogramaEstados.COMPLETADO, CronogramaEstados.CANCELADO -> false;
            default -> false;
        };

        if (!permitida) {
            throw new CronogramaException(
                    CronogramaErrorCode.TRANSICION_INVALIDA,
                    "Transición de cronograma inválida: " + estadoActual + " -> " + estadoNuevo);
        }
    }

    /**
     * Valida que una actividad pueda ser editada según su estado actual.
     * Las actividades en COMPLETADO o CANCELADO no son editables.
     *
     * @param estadoActual estado actual de la actividad
     * @throws CronogramaException si la actividad no es editable
     */
    public void validarActividadEditable(String estadoActual) {
        if (CronogramaEstados.COMPLETADO.equals(estadoActual)
                || CronogramaEstados.CANCELADO.equals(estadoActual)) {
            throw new CronogramaException(
                    CronogramaErrorCode.NO_EDITABLE,
                    "No se puede editar una actividad en estado: " + estadoActual);
        }
    }

    /**
     * Valida que el evento permita operaciones de cronograma.
     * Solo eventos en PROGRAMADO, CONFIRMADO o EN_MONTAJE pueden tener actividades.
     *
     * @param evento evento a validar
     * @throws CronogramaException si el evento no está en un estado válido
     */
    public void validarEventoPermiteCronograma(Evento evento) {
        String estadoEvento = evento.getEstadoEvento().getNombre();
        if (!ESTADOS_EVENTO_PERMITIDOS.contains(estadoEvento)) {
            throw new CronogramaException(
                    CronogramaErrorCode.EVENTO_NO_PERMITE_EDICION,
                    "Solo se pueden crear o editar actividades en eventos PROGRAMADO, CONFIRMADO o EN_MONTAJE. Estado actual: "
                            + estadoEvento);
        }
    }

    /**
     * Valida que las fechas de una actividad sean coherentes y estén dentro del rango del evento.
     * <ul>
     *   <li>La fecha de inicio debe ser anterior a la fecha de fin.</li>
     *   <li>Ambas fechas deben caer en el mismo día calendario que el evento.</li>
     *   <li>Los horarios deben estar dentro del horario del evento.</li>
     * </ul>
     *
     * @param fechaInicio fecha y hora de inicio de la actividad
     * @param fechaFin    fecha y hora de fin de la actividad
     * @param evento      evento al que pertenece la actividad
     * @throws CronogramaException si las fechas son inválidas
     */
    public void validarFechasActividad(LocalDateTime fechaInicio,
                                       LocalDateTime fechaFin,
                                       Evento evento) {
        if (!fechaInicio.isBefore(fechaFin)) {
            throw new CronogramaException(
                    CronogramaErrorCode.FECHA_INVALIDA,
                    "La fecha de inicio debe ser anterior a la fecha de fin");
        }

        LocalDate fechaEvento = evento.getFecha();
        if (fechaEvento != null
                && (!fechaInicio.toLocalDate().equals(fechaEvento)
                || !fechaFin.toLocalDate().equals(fechaEvento))) {
            throw new CronogramaException(
                    CronogramaErrorCode.FUERA_RANGO_EVENTO,
                    "Las fechas de la actividad deben estar dentro del día del evento: " + fechaEvento);
        }

        LocalTime horaInicioEvento = evento.getHoraInicio();
        LocalTime horaFinEvento = evento.getHoraFin();
        if (horaInicioEvento != null && horaFinEvento != null) {
            if (fechaInicio.toLocalTime().isBefore(horaInicioEvento)
                    || fechaFin.toLocalTime().isAfter(horaFinEvento)) {
                throw new CronogramaException(
                        CronogramaErrorCode.FUERA_RANGO_EVENTO,
                        "La actividad debe estar dentro del horario del evento: "
                                + horaInicioEvento + " - " + horaFinEvento);
            }
        }
    }

    /**
     * Indica si una actividad es editable según su estado actual.
     *
     * @param estadoActual estado actual de la actividad
     * @return {@code true} si no está en estado terminal (COMPLETADO o CANCELADO)
     */
    public boolean isEditable(String estadoActual) {
        return !CronogramaEstados.COMPLETADO.equals(estadoActual)
                && !CronogramaEstados.CANCELADO.equals(estadoActual);
    }

    /**
     * Indica si una actividad puede ser cancelada según su estado actual.
     *
     * @param estadoActual estado actual de la actividad
     * @return {@code true} si está en PENDIENTE o EN_PROGRESO
     */
    public boolean isCancelable(String estadoActual) {
        return CronogramaEstados.PENDIENTE.equals(estadoActual)
                || CronogramaEstados.EN_PROGRESO.equals(estadoActual);
    }

    /**
     * Retorna las acciones de UI permitidas para el estado actual.
     *
     * @param estadoActual estado actual de la actividad
     * @return lista de acciones (INICIAR, COMPLETAR, CANCELAR) o vacía para estados terminales
     */
    public List<String> getAccionesPermitidas(String estadoActual) {
        return switch (estadoActual) {
            case CronogramaEstados.PENDIENTE -> List.of(CronogramaAcciones.INICIAR, CronogramaAcciones.CANCELAR);
            case CronogramaEstados.EN_PROGRESO -> List.of(CronogramaAcciones.COMPLETAR, CronogramaAcciones.CANCELAR);
            default -> List.of();
        };
    }

    /**
     * Retorna los estados destino permitidos desde el estado actual.
     *
     * @param estadoActual estado actual de la actividad
     * @return lista de estados siguientes o vacía para estados terminales
     */
    public List<String> getEstadosSiguientesPermitidos(String estadoActual) {
        return switch (estadoActual) {
            case CronogramaEstados.PENDIENTE -> List.of(CronogramaEstados.EN_PROGRESO, CronogramaEstados.CANCELADO);
            case CronogramaEstados.EN_PROGRESO -> List.of(CronogramaEstados.COMPLETADO, CronogramaEstados.CANCELADO);
            default -> List.of();
        };
    }
}
