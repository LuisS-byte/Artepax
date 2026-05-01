package com.catolica.Artepax.common.job;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class JobCancelacionesPendientes {private static final Logger log = LoggerFactory.getLogger(JobCancelacionesPendientes.class);

    private final EventoRepository eventoRepository;
    private final EventoService eventoService;
    private final ConfiguracionService configuracionService;
    private final NotificacionService notificacionService;

    public JobCancelacionesPendientes(EventoRepository eventoRepository,
                                      EventoService eventoService,
                                      ConfiguracionService configuracionService,
                                      NotificacionService notificacionService) {
        this.eventoRepository = eventoRepository;
        this.eventoService = eventoService;
        this.configuracionService = configuracionService;
        this.notificacionService = notificacionService;
    }

    // Corre cada minuto
    @Scheduled(fixedDelayString = "${artepax.job.cancelacion.intervalo-ms:60000}")
    public void procesarCancelacionesPendientes() {
        int minutosGracia = configuracionService.getInt("TIEMPO_GRACIA_CANCELACION_MINUTOS");
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosGracia);

        List<Evento> pendientes = eventoRepository.findEnCancelacionPendiente();

        for (Evento evento : pendientes) {
            if (evento.getFechaCancelacion() != null &&
                    evento.getFechaCancelacion().isBefore(limite)) {
                try {
                    eventoService.confirmarCancelacion(evento.getId());
                    log.info("Evento {} cancelado automáticamente por tiempo de gracia expirado",
                            evento.getId());

                    notificacionService.notificarAdmins(
                            "CANCELACION_EVENTO",
                            "El evento #" + evento.getId() + " fue cancelado automáticamente " +
                                    "por tiempo de gracia expirado. Motivo: " + evento.getMotivoCancelacion(),
                            evento);

                } catch (Exception e) {
                    log.error("Error al cancelar evento {}: {}", evento.getId(), e.getMessage());
                }
            }
        }
    }
}