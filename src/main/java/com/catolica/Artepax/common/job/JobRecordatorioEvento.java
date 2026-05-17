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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Job que genera recordatorios de eventos próximos.
 * Se ejecuta diariamente a las 8:00 AM.
 * Lee DIAS_RECORDATORIO_EVENTO de configuración para calcular la fecha objetivo.
 * Incluye control de idempotencia para evitar notificaciones duplicadas
 * y JOIN FETCH para evitar LazyInitializationException en relaciones.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobRecordatorioEvento {

    private static final String TIPO_NOTIFICACION = "RECORDATORIO_EVENTO";
    private static final List<String> ESTADOS_EXCLUIDOS = List.of(
            "CANCELADO", "FINALIZADO", "CANCELACION_PENDIENTE", "PENDIENTE_COBRO");

    private final VistaEventosPeriodoRepository vistaRepository;
    private final EventoRepository eventoRepository;
    private final ConfiguracionService configuracionService;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    @Scheduled(cron = "0 0 8 * * *")
    public void procesarRecordatorios() {
        int diasAviso = configuracionService.getInt("DIAS_RECORDATORIO_EVENTO");
        LocalDate fechaObjetivo = LocalDate.now().plusDays(diasAviso);
        LocalDateTime inicioDeHoy = LocalDateTime.now().with(LocalTime.MIN);

        List<VistaEventosPeriodo> eventos =
                vistaRepository.findByFechaEventoBetween(fechaObjetivo, fechaObjetivo);

        for (VistaEventosPeriodo vista : eventos) {
            try {
                if (!estadoActivo(vista)) {
                    continue;
                }

                if (notificacionRepository.existsByTipoAndEventoSince(
                        TIPO_NOTIFICACION, vista.getIdEvento(), inicioDeHoy)) {
                    log.debug("Recordatorio para evento {} ya notificado hoy, saltando", vista.getIdEvento());
                    continue;
                }

                Optional<Evento> eventoOpt = eventoRepository.findByIdWithClienteAndEstado(vista.getIdEvento());
                eventoOpt.ifPresentOrElse(
                        evento -> {
                            notificacionService.notificarAdmins(
                                    TIPO_NOTIFICACION,
                                    "Recordatorio: el evento #" + evento.getId() +
                                            " del cliente " + evento.getCliente().getNombre() +
                                            " se realizará el " + evento.getFecha() +
                                            " en " + evento.getUbicacion() +
                                            ". Estado actual: " + evento.getEstadoEvento().getNombre(),
                                    evento
                            );
                            log.info("Recordatorio generado para evento {}", evento.getId());
                        },
                        () -> log.warn("Evento {} no encontrado en tabla evento (solo en vista)",
                                vista.getIdEvento())
                );

            } catch (Exception e) {
                log.error("Error procesando recordatorio evento {}: {}",
                        vista.getIdEvento(), e.getMessage(), e);
            }
        }
    }

    private boolean estadoActivo(VistaEventosPeriodo vista) {
        return !ESTADOS_EXCLUIDOS.contains(vista.getEstado());
    }
}
