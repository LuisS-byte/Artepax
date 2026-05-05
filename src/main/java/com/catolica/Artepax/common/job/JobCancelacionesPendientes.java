package com.catolica.Artepax.common.job;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job que procesa eventos en CANCELACION_PENDIENTE cuyo tiempo de gracia expiró.
 * Se ejecuta cada minuto (configurable via {@code artepax.job.cancelacion.intervalo-ms}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobCancelacionesPendientes {

    private final EventoRepository eventoRepository;
    private final EventoService eventoService;
    private final ConfiguracionService configuracionService;
    private final NotificacionService notificacionService;

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
