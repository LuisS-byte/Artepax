package com.catolica.Artepax.common.job;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.VistaEventosPeriodo;
import com.catolica.Artepax.evento.repository.VistaEventosPeriodoRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class JobRecordatorioEvento {
    private static final Logger log = LoggerFactory.getLogger(JobRecordatorioEvento.class);

    private final VistaEventosPeriodoRepository vistaRepository;
    private final ConfiguracionService configuracionService;
    private final NotificacionService notificacionService;

    public JobRecordatorioEvento(VistaEventosPeriodoRepository vistaRepository,
                                 ConfiguracionService configuracionService,
                                 NotificacionService notificacionService) {
        this.vistaRepository = vistaRepository;
        this.configuracionService = configuracionService;
        this.notificacionService = notificacionService;
    }

    // Corre diario a las 8am
    @Scheduled(cron = "0 0 8 * * *")
    public void procesarRecordatorios() {
        int diasAviso = configuracionService.getInt("DIAS_RECORDATORIO_EVENTO");
        LocalDate fechaObjetivo = LocalDate.now().plusDays(diasAviso);

        // Buscar eventos cuya fecha sea exactamente en X días
        List<VistaEventosPeriodo> eventos =
                vistaRepository.findByFechaEventoBetween(fechaObjetivo, fechaObjetivo);

        for (VistaEventosPeriodo evento : eventos) {
            try {
                // Solo eventos activos — no cancelados ni finalizados
                boolean estadoActivo = !List.of("CANCELADO", "FINALIZADO", "CANCELACION_PENDIENTE")
                        .contains(evento.getEstado());

                if (estadoActivo) {
                    notificacionService.notificarAdmins(
                            "RECORDATORIO_EVENTO",
                            "Recordatorio: el evento #" + evento.getIdEvento() +
                                    " del cliente " + evento.getClienteNombre() +
                                    " se realizará el " + evento.getFechaEvento() +
                                    " en " + evento.getUbicacion() +
                                    ". Estado actual: " + evento.getEstado(),
                            null
                    );

                    log.info("Recordatorio generado para evento {}", evento.getIdEvento());
                }

            } catch (Exception e) {
                log.error("Error procesando recordatorio evento {}: {}",
                        evento.getIdEvento(), e.getMessage());
            }
        }
    }
}