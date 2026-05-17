package com.catolica.Artepax.evento.service.handler;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.pago.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Handler para validaciones de pago relacionadas con eventos.
 * <p>
 * Centraliza la validación del anticipo mínimo usando la configuración
 * {@code PORCENTAJE_ANTICIPO_MINIMO} (NOTAS_DB Sec. 10).
 */
@Component
public class EventoPagoValidator {

    private static final Logger log = LoggerFactory.getLogger(EventoPagoValidator.class);

    private final ConfiguracionService configuracionService;
    private final PagoRepository pagoRepository;

    public EventoPagoValidator(ConfiguracionService configuracionService,
                               PagoRepository pagoRepository) {
        this.configuracionService = configuracionService;
        this.pagoRepository = pagoRepository;
    }

    /**
     * Valida que el total pagado (estado PAGADO) alcance el porcentaje mínimo
     * configurado para confirmar un evento.
     *
     * @param evento evento a validar
     * @throws EventoException si el anticipo no alcanza el mínimo requerido
     */
    public void validarAnticipoMinimo(Evento evento) {
        BigDecimal anticipoMinimo = calcularAnticipoMinimo(evento);
        BigDecimal totalPagado = getTotalPagado(evento.getId());

        if (totalPagado.compareTo(anticipoMinimo) < 0) {
            throw new EventoException(
                    "No se puede confirmar el evento #" + evento.getId() +
                            ". Anticipo mínimo requerido: $" + anticipoMinimo +
                            ", total pagado: $" + totalPagado);
        }

        log.info("anticipo_validado eventoId={} minimo={} pagado={}",
                evento.getId(), anticipoMinimo, totalPagado);
    }

    /**
     * Calcula el monto mínimo de anticipo requerido para confirmar un evento.
     * <p>
     * El porcentaje se obtiene de la configuración del sistema
     * ({@code PORCENTAJE_ANTICIPO_MINIMO}) y se aplica sobre el precio total.
     *
     * @param evento evento a evaluar
     * @return monto mínimo requerido
     */
    public BigDecimal calcularAnticipoMinimo(Evento evento) {
        BigDecimal porcentaje = configuracionService.getDecimal("PORCENTAJE_ANTICIPO_MINIMO");

        return evento.getPrecioTotal()
                .multiply(porcentaje)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Verifica si el total pagado de un evento alcanza o supera el anticipo mínimo.
     *
     * @param evento evento a evaluar
     * @return true si el anticipo es suficiente para confirmar
     */
    public boolean tieneAnticipoSuficiente(Evento evento) {
        return getTotalPagado(evento.getId()).compareTo(calcularAnticipoMinimo(evento)) >= 0;
    }

    /**
     * Retorna el total cobrado para un evento (pagos en estado PAGADO).
     */
    public BigDecimal getTotalPagado(Long eventoId) {
        return pagoRepository.sumMontoConfirmadoByEventoId(eventoId);
    }
}
