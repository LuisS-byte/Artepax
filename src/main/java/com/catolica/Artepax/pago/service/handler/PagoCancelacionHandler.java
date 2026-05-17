package com.catolica.Artepax.pago.service.handler;

import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import com.catolica.Artepax.pago.entity.EstadoPago;
import com.catolica.Artepax.pago.entity.Pago;
import com.catolica.Artepax.pago.exception.EstadoPagoNoConfiguradoException;
import com.catolica.Artepax.pago.repository.EstadoPagoRepository;
import com.catolica.Artepax.pago.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handles payment state transitions when an event is cancelled.
 * <p>
 * Conversion rules:
 * <ul>
 *   <li>{@code PENDIENTE} → {@code CANCELADO} — no financial impact, the payment
 *       was never confirmed</li>
 *   <li>{@code PAGADO} → {@code DEVOLUCION_PENDIENTE} — the payment was confirmed
 *       and requires manual admin processing to issue a refund</li>
 * </ul>
 * Notifies administrators when there are pending refunds.
 */
@Component
public class PagoCancelacionHandler {

    private static final Logger log = LoggerFactory.getLogger(PagoCancelacionHandler.class);

    private final PagoRepository pagoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final NotificacionService notificacionService;

    public PagoCancelacionHandler(PagoRepository pagoRepository,
                                  EstadoPagoRepository estadoPagoRepository,
                                  NotificacionService notificacionService) {
        this.pagoRepository = pagoRepository;
        this.estadoPagoRepository = estadoPagoRepository;
        this.notificacionService = notificacionService;
    }

    /**
     * Procesa los pagos de un evento que se está cancelando.
     *
     * @param evento el evento que se cancela
     * @param motivo motivo de cancelación para el log
     */
    @Transactional
    public BigDecimal procesarPagosPorCancelacion(Evento evento, String motivo) {
        Long eventoId = evento.getId();
        List<String> estadosActivos = List.of("PENDIENTE", "PAGADO");

        List<Pago> pagos = pagoRepository.findByEventoIdAndEstadoPago_EstadoNombreIn(eventoId, estadosActivos);

        if (pagos.isEmpty()) {
            log.info("cancelacion_pago_sin_pagos eventoId={}", eventoId);
            return BigDecimal.ZERO;
        }

        EstadoPago estadoCancelado = estadoPagoRepository.findByEstadoNombre("CANCELADO")
                .orElseThrow(() -> new EstadoPagoNoConfiguradoException("CANCELADO"));

        boolean hayPagados = pagos.stream()
                .anyMatch(p -> "PAGADO".equals(p.getEstadoPago().getEstadoNombre()));

        EstadoPago estadoDevolucion = null;
        if (hayPagados) {
            estadoDevolucion = estadoPagoRepository.findByEstadoNombre("DEVOLUCION_PENDIENTE")
                    .orElseThrow(() -> new EstadoPagoNoConfiguradoException("DEVOLUCION_PENDIENTE"));
        }

        BigDecimal totalDevolucion = BigDecimal.ZERO;

        for (Pago pago : pagos) {
            String estadoActual = pago.getEstadoPago().getEstadoNombre();

            if ("PENDIENTE".equals(estadoActual)) {
                pago.setEstadoPago(estadoCancelado);
                log.info("cancelacion_pago_pendiente eventoId={} pagoId={}", eventoId, pago.getId());
            } else if ("PAGADO".equals(estadoActual)) {
                pago.setEstadoPago(estadoDevolucion);
                totalDevolucion = totalDevolucion.add(pago.getMonto());
                log.info("cancelacion_pago_devolucion eventoId={} pagoId={} monto={}",
                        eventoId, pago.getId(), pago.getMonto());
            }
        }

        pagoRepository.saveAll(pagos);

        if (totalDevolucion.compareTo(BigDecimal.ZERO) > 0) {
            String mensaje = "El evento #" + eventoId + " - " + evento.getCliente().getNombre() +
                    " fue cancelado. Monto pendiente de devoluci\u00f3n: $" + totalDevolucion +
                    ". Motivo: " + motivo;

            notificacionService.notificarAdmins("CANCELACION_EVENTO", mensaje, evento);
            log.warn("cancelacion_notificacion_devolucion eventoId={} total={}", eventoId, totalDevolucion);
        }

        return totalDevolucion;
    }
}
