package com.catolica.Artepax.pago.service.impl;

import com.catolica.Artepax.evento.exception.EventoNotFoundException;
import com.catolica.Artepax.evento.model.EstadoEventoNames;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.evento.service.handler.EventoPagoValidator;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import com.catolica.Artepax.pago.exception.ConceptoPagoNotFoundException;
import com.catolica.Artepax.pago.exception.EstadoPagoNoConfiguradoException;
import com.catolica.Artepax.pago.exception.MetodoPagoNotFoundException;
import com.catolica.Artepax.pago.exception.PagoException;
import com.catolica.Artepax.pago.exception.PagoNotFoundException;
import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;
import com.catolica.Artepax.pago.entity.*;
import com.catolica.Artepax.pago.mapper.PagoMapper;
import com.catolica.Artepax.pago.repository.*;
import com.catolica.Artepax.pago.service.PagoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PagoServiceImpl implements PagoService {


    private final PagoRepository pagoRepository;
    private final PagoMapper pagoMapper;
    private final EventoRepository eventoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final ConceptoPagoRepository conceptoPagoRepository;
    private final EventoService eventoService;
    private final NotificacionService notificacionService;
    private final VistaPagosEventoRepository vistaPagosEventoRepository; // P10
    private final EventoPagoValidator eventoPagoValidator;

    private static final Set<String> ESTADOS_EVENTO_PERMITIDOS_PARA_CONFIRMAR = Set.of(
            EstadoEventoNames.COTIZACION,
            EstadoEventoNames.PROGRAMADO,
            EstadoEventoNames.CONFIRMADO,
            EstadoEventoNames.EN_MONTAJE
    );

    public PagoServiceImpl(PagoRepository pagoRepository,
                           PagoMapper pagoMapper,
                           EventoRepository eventoRepository,
                           EstadoPagoRepository estadoPagoRepository,
                           MetodoPagoRepository metodoPagoRepository,
                           ConceptoPagoRepository conceptoPagoRepository,
                           EventoService eventoService,
                           NotificacionService notificacionService,
                           VistaPagosEventoRepository vistaPagosEventoRepository,
                           EventoPagoValidator eventoPagoValidator) {
        this.pagoRepository = pagoRepository;
        this.pagoMapper = pagoMapper;
        this.eventoRepository = eventoRepository;
        this.estadoPagoRepository = estadoPagoRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.conceptoPagoRepository = conceptoPagoRepository;
        this.eventoService = eventoService;
        this.notificacionService = notificacionService;
        this.vistaPagosEventoRepository = vistaPagosEventoRepository;
        this.eventoPagoValidator = eventoPagoValidator;
    }

    @Override
    @Transactional
    public PagoResponseDTO registrarPago(PagoRequestDTO dto) {

        Evento evento = eventoRepository.findById(dto.getEventoId())
                .orElseThrow(() -> new EventoNotFoundException(dto.getEventoId()));

        String estadoActual = evento.getEstadoEvento().getNombre();
        if (estadoActual.equals("CANCELADO")
                || estadoActual.equals("CANCELACION_PENDIENTE")
                || estadoActual.equals("FINALIZADO")) {
            throw new PagoException(
                    "No se pueden registrar pagos en eventos con estado: " + estadoActual);
        }

        BigDecimal totalPagado = pagoRepository.sumMontoConfirmadoByEventoId(dto.getEventoId());
        BigDecimal precioTotal = evento.getPrecioTotal();
        if (totalPagado.add(dto.getMonto()).compareTo(precioTotal) > 0) {
            throw new PagoException(
                    "El pago excede el precio total del evento. " +
                            "Saldo pendiente: " + precioTotal.subtract(totalPagado));
        }

        EstadoPago estadoPendiente = estadoPagoRepository.findByEstadoNombre("PENDIENTE")
                .orElseThrow(() -> new EstadoPagoNoConfiguradoException("PENDIENTE"));

        MetodoPago metodo = metodoPagoRepository.findById(dto.getMetodoPagoId())
                .orElseThrow(() -> new MetodoPagoNotFoundException(dto.getMetodoPagoId()));

        if (!Boolean.TRUE.equals(metodo.getMetodoActivo())) {
            throw new PagoException("El método de pago está inactivo");
        }

        ConceptoPago concepto = conceptoPagoRepository.findById(dto.getConceptoPagoId())
                .orElseThrow(() -> new ConceptoPagoNotFoundException(dto.getConceptoPagoId()));

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
                .orElseThrow(() -> new PagoNotFoundException(pagoId));

        if (!pago.getEstadoPago().getEstadoNombre().equals("PENDIENTE")) {
            throw new PagoException(
                    "Solo se pueden confirmar pagos en estado PENDIENTE");
        }

        Evento eventoActualizado = eventoRepository.findById(pago.getEvento().getId())
                .orElseThrow(() -> new EventoNotFoundException(pago.getEvento().getId()));

        String estadoEventoActual = eventoActualizado.getEstadoEvento().getNombre();
        if (!ESTADOS_EVENTO_PERMITIDOS_PARA_CONFIRMAR.contains(estadoEventoActual)) {
            throw new PagoException(
                    "No se pueden confirmar pagos en eventos con estado: " + estadoEventoActual);
        }

        BigDecimal totalConfirmadoSinPagoActual = pagoRepository
                .sumPagosConfirmadosPorEventoExcluyendoPago(eventoActualizado.getId(), pago.getId());

        if (totalConfirmadoSinPagoActual.add(pago.getMonto())
                .compareTo(eventoActualizado.getPrecioTotal()) > 0) {
            throw new PagoException(
                    "El pago excede el precio total del evento. Saldo pendiente: "
                            + eventoActualizado.getPrecioTotal().subtract(totalConfirmadoSinPagoActual));
        }

        EstadoPago estadoPagado = estadoPagoRepository.findByEstadoNombre("PAGADO")
                .orElseThrow(() -> new EstadoPagoNoConfiguradoException("PAGADO"));

        pago.setEstadoPago(estadoPagado);
        pago.setFecha(LocalDateTime.now());
        pagoRepository.save(pago);

        Evento evento = eventoActualizado;

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
                .orElseThrow(() -> new PagoNotFoundException(pagoId));

        String estadoActual = pago.getEstadoPago().getEstadoNombre();
        if (!estadoActual.equals("PENDIENTE")) {
            throw new PagoException(
                    "Solo se pueden anular pagos en estado PENDIENTE");
        }

        EstadoPago estadoAnulado = estadoPagoRepository.findByEstadoNombre("CANCELADO")
                .orElseThrow(() -> new EstadoPagoNoConfiguradoException("CANCELADO"));

        pago.setEstadoPago(estadoAnulado);
        return pagoMapper.toDTO(pagoRepository.save(pago));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponseDTO> obtenerPorEvento(Long eventoId) {
        return pagoRepository.findByEventoIdWithRelations(eventoId)
                .stream().map(pagoMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PagoResponseDTO> obtenerTodos(Pageable pageable) {
        return pagoRepository.findAllWithEvento(pageable)
                .map(pagoMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenPagoEventoDTO getResumenPagos(Long eventoId) {
        return vistaPagosEventoRepository.findByIdEvento(eventoId)
                .map(vista -> new ResumenPagoEventoDTO(
                        vista.getIdEvento(),
                        vista.getPrecioTotal(),
                        vista.getTotalCobrado(),
                        vista.getSaldoPendiente(),
                        vista.getCobroCompleto()))
                .orElseGet(() -> {
                    Evento evento = eventoRepository.findById(eventoId)
                            .orElseThrow(() -> new EventoNotFoundException(eventoId));
                    return new ResumenPagoEventoDTO(
                            eventoId,
                            evento.getPrecioTotal(),
                            BigDecimal.ZERO,
                            evento.getPrecioTotal(),
                            false);
                });
    }

    // Auto-transición PROGRAMADO → CONFIRMADO si anticipo mínimo cumplido
    // No lanza excepción si no alcanza — es seguro llamarlo tras confirmar un pago
    private void verificarAnticipo(Evento evento) {
        if (!evento.getEstadoEvento().getNombre().equals(EstadoEventoNames.PROGRAMADO)) {
            return;
        }

        if (eventoPagoValidator.tieneAnticipoSuficiente(evento)) {
            eventoService.confirmarDesdeAnticipo(evento.getId());
        }
    }
}
