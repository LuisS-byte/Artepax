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
            EstadoEventoNames.CONFIRMADO,
            EstadoEventoNames.EN_MONTAJE
    );

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

    public void validarActividadEditable(String estadoActual) {
        if (CronogramaEstados.COMPLETADO.equals(estadoActual)
                || CronogramaEstados.CANCELADO.equals(estadoActual)) {
            throw new CronogramaException(
                    CronogramaErrorCode.NO_EDITABLE,
                    "No se puede editar una actividad en estado: " + estadoActual);
        }
    }

    public void validarEventoPermiteCronograma(Evento evento) {
        String estadoEvento = evento.getEstadoEvento().getNombre();
        if (!ESTADOS_EVENTO_PERMITIDOS.contains(estadoEvento)) {
            throw new CronogramaException(
                    CronogramaErrorCode.EVENTO_NO_PERMITE_EDICION,
                    "Solo se pueden crear o editar actividades en eventos CONFIRMADO o EN_MONTAJE. Estado actual: "
                            + estadoEvento);
        }
    }

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

    public boolean isEditable(String estadoActual) {
        return !CronogramaEstados.COMPLETADO.equals(estadoActual)
                && !CronogramaEstados.CANCELADO.equals(estadoActual);
    }

    public boolean isCancelable(String estadoActual) {
        return CronogramaEstados.PENDIENTE.equals(estadoActual)
                || CronogramaEstados.EN_PROGRESO.equals(estadoActual);
    }

    public List<String> getAccionesPermitidas(String estadoActual) {
        return switch (estadoActual) {
            case CronogramaEstados.PENDIENTE -> List.of(CronogramaAcciones.INICIAR, CronogramaAcciones.CANCELAR);
            case CronogramaEstados.EN_PROGRESO -> List.of(CronogramaAcciones.COMPLETAR, CronogramaAcciones.CANCELAR);
            default -> List.of();
        };
    }

    public List<String> getEstadosSiguientesPermitidos(String estadoActual) {
        return switch (estadoActual) {
            case CronogramaEstados.PENDIENTE -> List.of(CronogramaEstados.EN_PROGRESO, CronogramaEstados.CANCELADO);
            case CronogramaEstados.EN_PROGRESO -> List.of(CronogramaEstados.COMPLETADO, CronogramaEstados.CANCELADO);
            default -> List.of();
        };
    }
}
