package com.catolica.Artepax.evento.service.handler;

import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Handler encargado de generar notificaciones relacionadas con eventos.
 * <p>
 * Centraliza la construcción de mensajes y delega en {@link NotificacionService}.
 * Los tipos de notificación usados corresponden al catálogo {@code tipo_notificacion}
 * documentado en la BD (NOTAS_DB Sec. 13).
 */
@Component
public class EventoNotificacionHandler {

    private static final Logger log = LoggerFactory.getLogger(EventoNotificacionHandler.class);

    private final NotificacionService notificacionService;

    public EventoNotificacionHandler(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /**
     * Notifica que un evento fue confirmado automáticamente por alcanzar el anticipo mínimo.
     * Tipo: {@code CONFIRMACION_RESERVA}
     */
    public void notificarConfirmacionAutomatica(Long eventoId, Evento evento) {
        notificacionService.notificarAdmins(
                "CONFIRMACION_RESERVA",
                "El evento #" + eventoId + " - " + evento.getCliente().getNombre() +
                        " alcanzó el anticipo mínimo y fue confirmado automáticamente.",
                evento);
        log.info("notificacion_confirmacion_automatica eventoId={}", eventoId);
    }

    /**
     * Notifica que un evento fue confirmado manualmente por un administrador.
     * Tipo: {@code CONFIRMACION_RESERVA}
     */
    public void notificarConfirmacionManual(Long eventoId, Evento evento) {
        notificacionService.notificarAdmins(
                "CONFIRMACION_RESERVA",
                "El evento #" + eventoId + " - " + evento.getCliente().getNombre() +
                        " fue confirmado manualmente. Fecha: " + evento.getFecha(),
                evento);
        log.info("notificacion_confirmacion_manual eventoId={}", eventoId);
    }

    /**
     * Notifica que un evento inició la fase de montaje.
     * Tipo: {@code INICIO_MONTAJE}
     */
    public void notificarInicioMontaje(Long eventoId, Evento evento) {
        notificacionService.notificarAdmins(
                "INICIO_MONTAJE",
                "El evento #" + eventoId + " - " + evento.getCliente().getNombre() +
                        " inició la fase de montaje. Fecha: " + evento.getFecha(),
                evento);
        log.info("notificacion_inicio_montaje eventoId={}", eventoId);
    }

    /**
     * Notifica que un evento se finalizó con saldo pendiente.
     * Tipo: {@code SALDO_PENDIENTE}
     */
    public void notificarSaldoPendiente(Long eventoId, Evento evento,
                                        BigDecimal totalPagado) {
        BigDecimal saldoPendiente = evento.getPrecioTotal().subtract(totalPagado);
        notificacionService.notificarAdmins(
                "SALDO_PENDIENTE",
                "El evento #" + eventoId + " - " + evento.getCliente().getNombre() +
                        " se finalizó con saldo pendiente de $" + saldoPendiente +
                        ". Total evento: $" + evento.getPrecioTotal() +
                        ", total cobrado: $" + totalPagado,
                evento);
        log.warn("notificacion_saldo_pendiente eventoId={} pendiente={}", eventoId, saldoPendiente);
    }

    /**
     * Notifica que se inició un proceso de cancelación con tiempo de gracia.
     * Tipo: {@code CANCELACION_EVENTO}
     */
    public void notificarCancelacionIniciada(Long eventoId, Evento evento, String motivo) {
        notificacionService.notificarAdmins(
                "CANCELACION_EVENTO",
                "El evento #" + eventoId + " - " + evento.getCliente().getNombre() +
                        " inició proceso de cancelación. Motivo: " + motivo +
                        ". Tiene tiempo de gracia para revertir.",
                evento);
        log.info("notificacion_cancelacion_iniciada eventoId={}", eventoId);
    }

    public void notificarCancelacionConfirmada(Long eventoId, Evento evento, String motivo) {
        notificacionService.notificarAdmins(
                "CANCELACION_EVENTO",
                "El evento #" + eventoId + " - " + evento.getCliente().getNombre() +
                        " fue cancelado definitivamente. Motivo: " + motivo,
                evento);
        log.info("notificacion_cancelacion_confirmada eventoId={}", eventoId);
    }
}
