package com.catolica.Artepax.pago.service.impl;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.exception.EventoNotFoundException;
import com.catolica.Artepax.evento.model.EstadoEventoNames;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.evento.service.handler.EventoPagoValidator;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import com.catolica.Artepax.pago.dto.AnticipoInfoDTO;
import com.catolica.Artepax.pago.dto.EventoCobranzaDTO;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final VistaPagosEventoRepository vistaPagosEventoRepository;
    private final EventoPagoValidator eventoPagoValidator;
    private final ConfiguracionService configuracionService;

    private static final Set<String> ESTADOS_EVENTO_PERMITIDOS_PARA_REGISTRAR = Set.of(
            EstadoEventoNames.PROGRAMADO,
            EstadoEventoNames.CONFIRMADO,
            EstadoEventoNames.EN_MONTAJE,
            EstadoEventoNames.PENDIENTE_COBRO
    );

    private static final Set<String> ESTADOS_EVENTO_PERMITIDOS_PARA_CONFIRMAR = Set.of(
            EstadoEventoNames.PROGRAMADO,
            EstadoEventoNames.CONFIRMADO,
            EstadoEventoNames.EN_MONTAJE,
            EstadoEventoNames.PENDIENTE_COBRO
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
                           EventoPagoValidator eventoPagoValidator,
                           ConfiguracionService configuracionService) {
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
        this.configuracionService = configuracionService;
    }

    @Override
    @Transactional
    public PagoResponseDTO registrarPago(PagoRequestDTO dto) {

        Evento evento = eventoRepository.findByIdForUpdate(dto.getEventoId())
                .orElseThrow(() -> new EventoNotFoundException(dto.getEventoId()));

        String estadoActual = evento.getEstadoEvento().getNombre();
        if (!ESTADOS_EVENTO_PERMITIDOS_PARA_REGISTRAR.contains(estadoActual)) {
            throw new PagoException(
                    "No se pueden registrar pagos en eventos con estado: " + estadoActual);
        }

        BigDecimal totalPagado = pagoRepository.sumMontoActivoByEventoId(dto.getEventoId());
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

        Pago pago = pagoRepository.findByIdWithRelations(pagoId)
                .orElseThrow(() -> new PagoNotFoundException(pagoId));

        if (!pago.getEstadoPago().getEstadoNombre().equals("PENDIENTE")) {
            throw new PagoException(
                    "Solo se pueden confirmar pagos en estado PENDIENTE");
        }

        Evento eventoActualizado = eventoRepository.findByIdForUpdate(pago.getEvento().getId())
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
        pagoRepository.flush(); // Forzar flush para que la vista_pagos_evento vea el pago como PAGADO

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

        // Verificar cobro completo → auto-transición PENDIENTE_COBRO → FINALIZADO
        eventoService.finalizarDesdeCobroCompleto(evento.getId());

        // Recargar pago para reflejar estado actualizado del evento
        // (evita devolver estadoEventoNombre desactualizado tras transiciones automáticas)
        Pago pagoRecargado = pagoRepository.findByIdWithRelations(pago.getId())
                .orElseThrow(() -> new PagoNotFoundException(pago.getId()));

        return pagoMapper.toDTO(pagoRecargado);
    }

    @Override
    @Transactional
    public PagoResponseDTO anularPago(Long pagoId) {

        Pago pago = pagoRepository.findByIdWithRelations(pagoId)
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
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new EventoNotFoundException(eventoId));

        AnticipoInfoDTO anticipo = calcularAnticipo(evento);

        return vistaPagosEventoRepository.findByIdEvento(eventoId)
                .map(vista -> new ResumenPagoEventoDTO(
                        vista.getIdEvento(),
                        vista.getPrecioTotal(),
                        vista.getTotalCobrado(),
                        vista.getSaldoPendiente(),
                        vista.getCobroCompleto(),
                        vista.getTotalDevolucionPendiente(),
                        vista.getTieneDevolucionPendiente(),
                        anticipo))
                .orElseGet(() -> new ResumenPagoEventoDTO(
                        eventoId,
                        evento.getPrecioTotal(),
                        BigDecimal.ZERO,
                        evento.getPrecioTotal(),
                        false,
                        BigDecimal.ZERO,
                        false,
                        anticipo));
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

    private AnticipoInfoDTO calcularAnticipo(Evento evento) {
        BigDecimal porcentaje = configuracionService.getDecimal("PORCENTAJE_ANTICIPO_MINIMO");
        BigDecimal montoMinimo = evento.getPrecioTotal()
                .multiply(porcentaje)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal anticipoConfirmado = eventoPagoValidator.getTotalPagado(evento.getId());
        BigDecimal anticipoPorConcepto = pagoRepository
                .sumMontoConfirmadoByEventoIdAndConceptoNombre(evento.getId(), "ANTICIPO");

        BigDecimal faltante = montoMinimo.subtract(anticipoConfirmado);
        if (faltante.compareTo(BigDecimal.ZERO) < 0) {
            faltante = BigDecimal.ZERO;
        }

        Boolean cumplido = anticipoConfirmado.compareTo(montoMinimo) >= 0;
        Boolean puedeConfirmar = cumplido && evento.getEstadoEvento().getNombre().equals(EstadoEventoNames.PROGRAMADO);

        return new AnticipoInfoDTO(
                porcentaje,
                montoMinimo,
                anticipoConfirmado,
                anticipoPorConcepto,
                faltante,
                cumplido,
                puedeConfirmar
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoCobranzaDTO> getEventosCobranza(Map<String, Object> filtros) {
        List<Object[]> raw = pagoRepository.findEventosCobranzaRaw();

        List<EventoCobranzaDTO> resultados = new ArrayList<>();
        for (Object[] row : raw) {
            EventoCobranzaDTO dto = new EventoCobranzaDTO(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (BigDecimal) row[3],
                    (BigDecimal) row[4],
                    (BigDecimal) row[5],
                    (Boolean) row[6],
                    (Boolean) row[7],
                    (BigDecimal) row[8],
                    ((Number) row[9]).intValue()
            );
            resultados.add(dto);
        }

        // Filtros
        @SuppressWarnings("unchecked")
        List<String> estados = (List<String>) filtros.get("estado");
        if (estados != null && !estados.isEmpty()) {
            resultados = resultados.stream()
                    .filter(e -> estados.contains(e.estadoEventoNombre()))
                    .collect(Collectors.toList());
        }

        Boolean soloConSaldo = (Boolean) filtros.get("soloConSaldo");
        if (Boolean.TRUE.equals(soloConSaldo)) {
            resultados = resultados.stream()
                    .filter(e -> e.saldoPendiente().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());
        }

        Boolean soloConDevolucion = (Boolean) filtros.get("soloConDevolucion");
        if (Boolean.TRUE.equals(soloConDevolucion)) {
            resultados = resultados.stream()
                    .filter(EventoCobranzaDTO::tieneDevolucionPendiente)
                    .collect(Collectors.toList());
        }

        Boolean soloConPagosPendientes = (Boolean) filtros.get("soloConPagosPendientes");
        if (Boolean.TRUE.equals(soloConPagosPendientes)) {
            resultados = resultados.stream()
                    .filter(e -> e.cantidadPagosPendientes() > 0)
                    .collect(Collectors.toList());
        }

        String q = (String) filtros.get("q");
        if (q != null && !q.isBlank()) {
            String busqueda = q.toLowerCase();
            resultados = resultados.stream()
                    .filter(e -> e.clienteNombre().toLowerCase().contains(busqueda)
                            || e.eventoId().toString().contains(busqueda))
                    .collect(Collectors.toList());
        }

        // Ordenamiento por prioridad
        resultados.sort(Comparator
                .comparing((EventoCobranzaDTO e) -> !e.tieneDevolucionPendiente())
                .thenComparing(e -> e.saldoPendiente().compareTo(BigDecimal.ZERO) <= 0)
                .thenComparing(e -> e.cantidadPagosPendientes() == 0)
                .thenComparing((EventoCobranzaDTO e) -> e.saldoPendiente().negate())
        );

        // Limit
        Integer limit = (Integer) filtros.get("limit");
        if (limit != null && limit > 0 && resultados.size() > limit) {
            resultados = resultados.subList(0, limit);
        }

        return resultados;
    }
}
