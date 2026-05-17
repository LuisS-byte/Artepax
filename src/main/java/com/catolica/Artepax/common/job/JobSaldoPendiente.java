package com.catolica.Artepax.common.job;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.VistaEventosPeriodo;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.VistaEventosPeriodoRepository;
import com.catolica.Artepax.notificacion.repository.NotificacionRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Job programado que alerta sobre eventos con saldo pendiente (estado
 * {@code PENDIENTE_COBRO} u otro activo) que están próximos a su fecha.
 * <p>
 * Se ejecuta diariamente a las 8:00 AM. Lee la clave de configuración
 * {@code DIAS_AVISO_SALDO_PENDIENTE} para definir el horizonte de búsqueda
 * (desde hoy hasta hoy + días de aviso).
 * <p>
 * Excluye eventos en estados terminales ({@code CANCELADO}, {@code FINALIZADO},
 * {@code CANCELACION_PENDIENTE}) y verifica que el saldo pendiente sea mayor a cero.
 * Incluye control de idempotencia y usa JOIN FETCH para evitar
 * LazyInitializationException.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobSaldoPendiente {

    private static final String TIPO_NOTIFICACION = "SALDO_PENDIENTE";
    private static final List<String> ESTADOS_EXCLUIDOS = List.of(
            "CANCELADO", "FINALIZADO", "CANCELACION_PENDIENTE");

    private final VistaEventosPeriodoRepository vistaRepository;
    private final EventoRepository eventoRepository;
    private final ConfiguracionService configuracionService;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    /**
     * Ejecuta el procesamiento de alertas de saldo pendiente.
     * Busca eventos próximos (hoy hasta hoy + {@code DIAS_AVISO_SALDO_PENDIENTE}),
     * filtra aquellos con saldo {@code > 0} y estados activos, y genera
     * notificaciones saltando las ya emitidas hoy.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void procesarSaldosPendientes() {
        int diasAviso = configuracionService.getInt("DIAS_AVISO_SALDO_PENDIENTE");
        LocalDate fechaLimite = LocalDate.now().plusDays(diasAviso);
        LocalDateTime inicioDeHoy = LocalDateTime.now().with(LocalTime.MIN);

        List<VistaEventosPeriodo> proximosEventos =
                vistaRepository.findByFechaEventoBetween(LocalDate.now(), fechaLimite);

        for (VistaEventosPeriodo vista : proximosEventos) {
            try {
                if (!tieneSaldoPendiente(vista) || !estadoActivo(vista)) {
                    continue;
                }

                if (notificacionRepository.existsByTipoAndEventoSince(
                        TIPO_NOTIFICACION, vista.getIdEvento(), inicioDeHoy)) {
                    log.debug("Alerta de saldo pendiente para evento {} ya notificada hoy, saltando",
                            vista.getIdEvento());
                    continue;
                }

                Optional<Evento> eventoOpt = eventoRepository.findByIdWithClienteAndEstado(vista.getIdEvento());
                eventoOpt.ifPresentOrElse(
                        evento -> {
                            notificacionService.notificarAdmins(
                                    TIPO_NOTIFICACION,
                                    "El evento #" + evento.getId() +
                                            " del cliente " + evento.getCliente().getNombre() +
                                            " tiene saldo pendiente de $" + vista.getSaldoPendiente() +
                                            ". Fecha del evento: " + evento.getFecha(),
                                    evento
                            );
                            log.info("Alerta de saldo pendiente generada para evento {}", evento.getId());
                        },
                        () -> log.warn("Evento {} no encontrado en tabla evento (solo en vista)",
                                vista.getIdEvento())
                );

            } catch (Exception e) {
                log.error("Error procesando saldo pendiente evento {}: {}",
                        vista.getIdEvento(), e.getMessage(), e);
            }
        }
    }

    private boolean tieneSaldoPendiente(VistaEventosPeriodo vista) {
        return vista.getSaldoPendiente() != null &&
                vista.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean estadoActivo(VistaEventosPeriodo vista) {
        return !ESTADOS_EXCLUIDOS.contains(vista.getEstado());
    }
}
