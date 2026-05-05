package com.catolica.Artepax.common.job;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.VistaEventosPeriodo;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.VistaEventosPeriodoRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Job que alerta sobre eventos con saldo pendiente próximos a su fecha.
 * Se ejecuta diariamente a las 8:00 AM.
 * Lee DIAS_AVISO_SALDO_PENDIENTE de configuración para definir el rango de búsqueda.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobSaldoPendiente {

    private final VistaEventosPeriodoRepository vistaRepository;
    private final EventoRepository eventoRepository;
    private final ConfiguracionService configuracionService;
    private final NotificacionService notificacionService;

    @Scheduled(cron = "0 0 8 * * *")
    public void procesarSaldosPendientes() {
        int diasAviso = configuracionService.getInt("DIAS_AVISO_SALDO_PENDIENTE");
        LocalDate fechaLimite = LocalDate.now().plusDays(diasAviso);

        List<VistaEventosPeriodo> proximosEventos =
                vistaRepository.findByFechaEventoBetween(LocalDate.now(), fechaLimite);

        for (VistaEventosPeriodo vista : proximosEventos) {
            try {
                boolean tieneSaldo = vista.getSaldoPendiente() != null &&
                        vista.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0;

                boolean estadoActivo = !List.of("CANCELADO", "FINALIZADO", "CANCELACION_PENDIENTE")
                        .contains(vista.getEstado());

                if (tieneSaldo && estadoActivo) {
                    Optional<Evento> eventoOpt = eventoRepository.findById(vista.getIdEvento());
                    eventoOpt.ifPresentOrElse(
                            evento -> {
                                notificacionService.notificarAdmins(
                                        "SALDO_PENDIENTE",
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
                }

            } catch (Exception e) {
                log.error("Error procesando saldo pendiente evento {}: {}",
                        vista.getIdEvento(), e.getMessage());
            }
        }
    }
}
