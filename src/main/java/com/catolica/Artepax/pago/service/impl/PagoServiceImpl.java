package com.catolica.Artepax.pago.service.impl;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;
import com.catolica.Artepax.pago.entity.*;
import com.catolica.Artepax.pago.mapper.PagoMapper;
import com.catolica.Artepax.pago.repository.*;
import com.catolica.Artepax.pago.service.PagoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.catolica.Artepax.evento.service.EventoService;
import org.springframework.context.annotation.Lazy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoServiceImpl implements PagoService {


    private final PagoRepository pagoRepository;
    private final PagoMapper pagoMapper;
    private final EventoRepository eventoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final ConceptoPagoRepository conceptoPagoRepository;
    private final ConfiguracionService configuracionService;
    private final EventoService eventoService;
    private final NotificacionService notificacionService;
    private final VistaPagosEventoRepository vistaPagosEventoRepository; // P10

    public PagoServiceImpl(PagoRepository pagoRepository,
                           PagoMapper pagoMapper,
                           EventoRepository eventoRepository,
                           EstadoPagoRepository estadoPagoRepository,
                           MetodoPagoRepository metodoPagoRepository,
                           ConceptoPagoRepository conceptoPagoRepository,
                           ConfiguracionService configuracionService,
                           @Lazy EventoService eventoService,
                           NotificacionService notificacionService,
                           VistaPagosEventoRepository vistaPagosEventoRepository  ) {
        this.pagoRepository = pagoRepository;
        this.pagoMapper = pagoMapper;
        this.eventoRepository = eventoRepository;
        this.estadoPagoRepository = estadoPagoRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.conceptoPagoRepository = conceptoPagoRepository;
        this.configuracionService = configuracionService;
        this.eventoService = eventoService;
        this.notificacionService = notificacionService;
        this.vistaPagosEventoRepository = vistaPagosEventoRepository;
    }

    @Override
    @Transactional
    public PagoResponseDTO registrarPago(PagoRequestDTO dto) {

        Evento evento = eventoRepository.findById(dto.getEventoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Evento no encontrado: " + dto.getEventoId()));

        String estadoActual = evento.getEstadoEvento().getNombre();
        if (estadoActual.equals("CANCELADO")
                || estadoActual.equals("CANCELACION_PENDIENTE")
                || estadoActual.equals("FINALIZADO")) {
            throw new IllegalArgumentException(
                    "No se pueden registrar pagos en eventos con estado: " + estadoActual);
        }

        BigDecimal totalPagado = pagoRepository.sumMontoConfirmadoByEventoId(dto.getEventoId());
        BigDecimal precioTotal = evento.getPrecioTotal();
        if (totalPagado.add(dto.getMonto()).compareTo(precioTotal) > 0) {
            throw new IllegalArgumentException(
                    "El pago excede el precio total del evento. " +
                            "Saldo pendiente: " + precioTotal.subtract(totalPagado));
        }

        EstadoPago estadoPendiente = estadoPagoRepository.findByEstadoNombre("PENDIENTE")
                .orElseThrow(() -> new IllegalStateException(
                        "Estado PENDIENTE no encontrado en BD"));

        MetodoPago metodo = metodoPagoRepository.findById(dto.getMetodoPagoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Método de pago no encontrado: " + dto.getMetodoPagoId()));

        ConceptoPago concepto = conceptoPagoRepository.findById(dto.getConceptoPagoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Concepto de pago no encontrado: " + dto.getConceptoPagoId()));

        Pago pago = new Pago();
        pago.setEvento(evento);
        pago.setMonto(dto.getMonto());
        pago.setEstadoPago(estadoPendiente);
        pago.setMetodoPago(metodo);
        pago.setConceptoPago(concepto);
        pago.setFecha(null);

        return pagoMapper.toDTO(pagoRepository.save(pago));
    }

    @Override
    @Transactional
    public PagoResponseDTO confirmarPago(Long pagoId) {

        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pago no encontrado: " + pagoId));

        if (!pago.getEstadoPago().getEstadoNombre().equals("PENDIENTE")) {
            throw new IllegalArgumentException(
                    "Solo se pueden confirmar pagos en estado PENDIENTE");
        }

        EstadoPago estadoPagado = estadoPagoRepository.findByEstadoNombre("PAGADO")
                .orElseThrow(() -> new IllegalStateException(
                        "Estado PAGADO no encontrado en BD"));

        pago.setEstadoPago(estadoPagado);
        pago.setFecha(LocalDateTime.now());
        pagoRepository.save(pago);

        Evento evento = pago.getEvento();

        // Notificar pago recibido
        notificacionService.notificarAdmins(
                "PAGO_RECIBIDO",
                "Pago de $" + pago.getMonto() +
                        " confirmado para el evento #" + evento.getId() +
                        " - " + evento.getCliente().getNombre(),
                evento);

        // Verificar anticipo → auto-transición a CONFIRMADO
        verificarAnticipo(evento);

        return pagoMapper.toDTO(pago);
    }

    @Override
    @Transactional
    public PagoResponseDTO anularPago(Long pagoId) {

        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pago no encontrado: " + pagoId));

        EstadoPago estadoAnulado = estadoPagoRepository.findByEstadoNombre("CANCELADO")
                .orElseThrow(() -> new IllegalStateException(
                        "Estado CANCELADO no encontrado en BD"));

        pago.setEstadoPago(estadoAnulado);
        return pagoMapper.toDTO(pagoRepository.save(pago));
    }

    @Override
    public List<PagoResponseDTO> obtenerPorEvento(Long eventoId) {
        return pagoRepository.findByEventoId(eventoId)
                .stream().map(pagoMapper::toDTO).toList();
    }

    @Override
    public ResumenPagoEventoDTO getResumenPagos(Long eventoId) {
        VistaPagosEvento vista = vistaPagosEventoRepository.findByIdEvento(eventoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró resumen para el evento " + eventoId));
        return new ResumenPagoEventoDTO(
                vista.getIdEvento(),
                vista.getPrecioTotal(),
                vista.getTotalCobrado(),
                vista.getSaldoPendiente(),
                vista.getCobroCompleto()
        );
    }

    // Auto-transición PROGRAMADO → CONFIRMADO si anticipo mínimo cumplido
    private void verificarAnticipo(Evento evento) {
        if (!evento.getEstadoEvento().getNombre().equals("PROGRAMADO")) return;

        int porcentaje = configuracionService.getInt("PORCENTAJE_ANTICIPO_MINIMO");
        BigDecimal minimo = evento.getPrecioTotal()
                .multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100));

        BigDecimal totalPagado = pagoRepository
                .sumMontoConfirmadoByEventoId(evento.getId());

        if (totalPagado.compareTo(minimo) >= 0) {
            eventoService.confirmarDesdeAnticipo(evento.getId());
        }
    }
}