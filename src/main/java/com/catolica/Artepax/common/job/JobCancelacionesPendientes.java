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
 * Job programado que procesa eventos en estado {@code CANCELACION_PENDIENTE}
 * cuyo tiempo de gracia ha expirado, transitándolos automáticamente a
 * {@code CANCELADO}.
 * <p>
 * Lee el tiempo de gracia configurable desde la base de datos mediante la
 * clave {@code TIEMPO_GRACIA_CANCELACION_MINUTOS} (fallback: 1440 minutos = 24 h).
 * Se ejecuta en intervalos fijos configurados por la propiedad
 * {@code artepax.job.cancelacion.intervalo-ms} (default: 60000 ms = 1 minuto).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobCancelacionesPendientes {

    private static final int FALLBACK_MINUTOS_GRACIA = 1440; // 24 horas

    private final EventoRepository eventoRepository;
    private final EventoService eventoService;
    private final ConfiguracionService configuracionService;

    /**
     * Ejecuta el procesamiento de cancelaciones pendientes.
     * Busca eventos en {@code CANCELACION_PENDIENTE}, compara su fecha de cancelación
     * contra el límite calculado con {@code TIEMPO_GRACIA_CANCELACION_MINUTOS},
     * y confirma la cancelación para aquellos cuyo tiempo de gracia expiró.
     */
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
     * Obtiene los minutos de gracia desde configuración {@code TIEMPO_GRACIA_CANCELACION_MINUTOS},
     * con fallback seguro si el valor falta o está mal formado.
     *
     * @return minutos de gracia configurados, o el fallback de 1440 si no se puede leer
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
