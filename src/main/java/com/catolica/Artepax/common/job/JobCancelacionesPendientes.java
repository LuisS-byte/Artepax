package com.catolica.Artepax.common.job;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
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

    private static final int FALLBACK_MINUTOS_GRACIA = 1440; // 24 horas

    private final EventoRepository eventoRepository;
    private final EventoService eventoService;
    private final ConfiguracionService configuracionService;

    @Scheduled(fixedDelayString = "${artepax.job.cancelacion.intervalo-ms:60000}")
    public void procesarCancelacionesPendientes() {
        int minutosGracia = obtenerMinutosGracia();
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosGracia);

        List<Evento> pendientes = eventoRepository.findEnCancelacionPendiente();

        for (Evento evento : pendientes) {
            if (evento.getFechaCancelacion() != null &&
                    evento.getFechaCancelacion().isBefore(limite)) {
                try {
                    eventoService.confirmarCancelacion(evento.getId());
                    log.info("Evento {} cancelado automáticamente por tiempo de gracia expirado",
                            evento.getId());

                } catch (Exception e) {
                    log.error("Error al cancelar evento {}: {}", evento.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Obtiene los minutos de gracia desde configuración, con fallback seguro
     * si el valor falta o está mal formado.
     */
    private int obtenerMinutosGracia() {
        try {
            return configuracionService.getInt("TIEMPO_GRACIA_CANCELACION_MINUTOS");
        } catch (Exception e) {
            log.warn("No se pudo leer TIEMPO_GRACIA_CANCELACION_MINUTOS, usando fallback de {} minutos: {}",
                    FALLBACK_MINUTOS_GRACIA, e.getMessage());
            return FALLBACK_MINUTOS_GRACIA;
        }
    }
}
