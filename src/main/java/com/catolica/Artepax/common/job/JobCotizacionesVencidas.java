package com.catolica.Artepax.common.job;

import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
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

/**
 * Job que notifica sobre cotizaciones vencidas.
 * Se ejecuta diariamente a las 8:00 AM.
 * Usa fechaExpiracion < hoy (estrictamente menor), no cambia el estado del evento.
 * Incluye control de idempotencia para evitar notificaciones duplicadas.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobCotizacionesVencidas {

    private static final String TIPO_NOTIFICACION = "COTIZACION_VENCIDA";

    private final EventoRepository eventoRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionService notificacionService;

    @Scheduled(cron = "0 0 8 * * *")
    public void procesarCotizacionesVencidas() {
        List<Evento> vencidas = eventoRepository.findCotizacionesVencidas(LocalDate.now());
        LocalDateTime inicioDeHoy = LocalDateTime.now().with(LocalTime.MIN);

        for (Evento evento : vencidas) {
            try {
                if (notificacionRepository.existsByTipoAndEventoSince(
                        TIPO_NOTIFICACION, evento.getId(), inicioDeHoy)) {
                    log.debug("Cotización vencida {} ya notificada hoy, saltando", evento.getId());
                    continue;
                }

                notificacionService.notificarAdmins(
                        TIPO_NOTIFICACION,
                        "La cotización #" + evento.getId() +
                                " del cliente " + evento.getCliente().getNombre() +
                                " venció el " + evento.getFechaExpiracion() +
                                ". Contactar para seguimiento.",
                        evento);

                log.info("Notificación generada para cotización vencida evento {}", evento.getId());

            } catch (Exception e) {
                log.error("Error procesando cotización vencida {}: {}", evento.getId(), e.getMessage(), e);
            }
        }
    }
}
