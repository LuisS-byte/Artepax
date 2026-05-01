package com.catolica.Artepax.common.job;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.VistaEventosPeriodo;
import com.catolica.Artepax.evento.repository.VistaEventosPeriodoRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class JobSaldoPendiente {

    private static final Logger log = LoggerFactory.getLogger(JobSaldoPendiente.class);

    private final VistaEventosPeriodoRepository vistaRepository;
    private final ConfiguracionService configuracionService;
    private final NotificacionService notificacionService;

    public JobSaldoPendiente(VistaEventosPeriodoRepository vistaRepository,
                             ConfiguracionService configuracionService,
                             NotificacionService notificacionService) {
        this.vistaRepository = vistaRepository;
        this.configuracionService = configuracionService;
        this.notificacionService = notificacionService;
    }

    // Corre diario a las 8am junto con el de cotizaciones
    @Scheduled(cron = "0 0 8 * * *")
    public void procesarSaldosPendientes() {
        int diasAviso = configuracionService.getInt("DIAS_AVISO_SALDO_PENDIENTE");
        LocalDate fechaLimite = LocalDate.now().plusDays(diasAviso);

        // Buscar eventos activos en el rango de hoy hasta fechaLimite
        List<VistaEventosPeriodo> proximosEventos =
                vistaRepository.findByFechaEventoBetween(LocalDate.now(), fechaLimite);

        for (VistaEventosPeriodo evento : proximosEventos) {
            try {
                // Solo eventos con saldo pendiente y que no estén cancelados/finalizados
                boolean tieneSaldo = evento.getSaldoPendiente() != null &&
                        evento.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0;

                boolean estadoActivo = !List.of("CANCELADO", "FINALIZADO", "CANCELACION_PENDIENTE")
                        .contains(evento.getEstado());

                if (tieneSaldo && estadoActivo) {
                    notificacionService.notificarAdmins(
                            "SALDO_PENDIENTE", // reutilizamos hasta que se agregue SALDO_PENDIENTE al catálogo
                            "El evento #" + evento.getIdEvento() +
                                    " del cliente " + evento.getClienteNombre() +
                                    " tiene saldo pendiente de $" + evento.getSaldoPendiente() +
                                    ". Fecha del evento: " + evento.getFechaEvento(),
                            null
                    );

                    log.info("Alerta de saldo pendiente generada para evento {}",
                            evento.getIdEvento());
                }

            } catch (Exception e) {
                log.error("Error procesando saldo pendiente evento {}: {}",
                        evento.getIdEvento(), e.getMessage());
            }
        }
    }

}
