package com.catolica.Artepax.common.job;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class JobCotizacionesVencidas {private static final Logger log = LoggerFactory.getLogger(JobCotizacionesVencidas.class);

    private final EventoRepository eventoRepository;
    private final NotificacionService notificacionService;

    public JobCotizacionesVencidas(EventoRepository eventoRepository,
                                   NotificacionService notificacionService) {
        this.eventoRepository = eventoRepository;
        this.notificacionService = notificacionService;
    }

    // Corre diario a las 8am
    @Scheduled(cron = "0 0 8 * * *")
    public void procesarCotizacionesVencidas() {
        List<com.catolica.Artepax.evento.model.Evento> vencidas =
                eventoRepository.findCotizacionesVencidas(LocalDate.now());

        for (var evento : vencidas) {
            try {
                notificacionService.notificarAdmins(
                        "COTIZACION_VENCIDA",
                        "La cotización #" + evento.getId() +
                                " del cliente " + evento.getCliente().getNombre() +
                                " venció el " + evento.getFechaExpiracion() +
                                ". Contactar para seguimiento.",
                        evento);

                log.info("Notificación generada para cotización vencida evento {}",
                        evento.getId());

            } catch (Exception e) {
                log.error("Error procesando cotización vencida {}: {}",
                        evento.getId(), e.getMessage());
            }
        }
    }
}