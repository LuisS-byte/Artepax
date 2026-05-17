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

/**
 * Core implementation of {@link PagoService} that coordinates the payment lifecycle.
 * <p>
 * Manages payment registration (PENDIENTE), confirmation (PAGADO), and cancellation (CANCELADO)
 * with the following event state transition side effects:
 * <ul>
 *   <li><strong>verificarAnticipo</strong> — after confirming a payment, if the event is in
 *       PROGRAMADO and the advance payment threshold is met, the event auto-transitions to
 *       CONFIRMADO</li>
 *   <li><strong>finalizarDesdeCobroCompleto</strong> — after confirming a payment, if the event
 *       is fully paid, it auto-transitions from PENDIENTE_COBRO to FINALIZADO</li>
 * </ul>
 * <p>
 * All write operations use pessimistic locking ({@code FOR UPDATE}) on the event row via
 * {@link EventoRepository#findByIdForUpdate(Long)} to prevent race conditions on balance checks.
 */
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

    /**
     * Registra un nuevo pago en estado PENDIENTE para un evento.
     * <p>
     * Valida que el evento esté en un estado permitido (PROGRAMADO, CONFIRMADO,
     * EN_MONTAJE, PENDIENTE_COBRO), que el método de pago esté activo, y que
     * el monto no exceda el saldo restante del evento. Usa bloqueo pesimista
     * ({@code FOR UPDATE}) sobre la fila del evento.
     *
     * @param dto datos del pago (evento, monto, método, concepto)
     * @return el pago creado en estado PENDIENTE
     * @throws PagoException si el evento no está en estado permitido, el método
     *                       está inactivo, o el monto excede el saldo disponible
     */
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

    /**
     * Confirma un pago pendiente, moviéndolo al estado PAGADO.
     * <p>
     * La confirmación establece la fecha de pago y fuerza un flush del
     * {@link PagoRepository} antes de leer la vista {@code vista_pagos_evento}.
     * <strong>No eliminar el {@code flush()}</strong> — la vista no se actualiza
     * hasta que la transacción actual se vacía al menos una vez, y una lectura
     * posterior devolvería datos obsoletos.
     * <p>
     * Después de confirmar el pago:
     * <ul>
     *   <li>Notifica a los administradores del pago recibido</li>
     *   <li>Verifica si el anticipo mínimo está cubierto → posible transición
     *       PROGRAMADO → CONFIRMADO</li>
     *   <li>Verifica si el evento quedó completamente pagado → posible transición
     *       PENDIENTE_COBRO → FINALIZADO</li>
     * </ul>
     * Recarga el pago antes de devolverlo para reflejar el estado actualizado
     * del evento tras las transiciones automáticas.
     *
     * @param pagoId ID del pago a confirmar
     * @return el pago confirmado con estado PAGADO e información del evento actualizada
     * @throws PagoNotFoundException si no se encuentra el pago
     * @throws PagoException         si el pago no está en PENDIENTE o el evento
     *                               no admite confirmaciones
     */
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

    /**
     * Anula un pago pendiente, moviéndolo al estado CANCELADO.
     * <p>
     * Solo se pueden anular pagos en estado PENDIENTE. Los pagos ya confirmados
     * (PAGADO) deben gestionarse mediante el flujo de cancelación de eventos.
     *
     * @param pagoId ID del pago a anular
     * @return el pago anulado con estado CANCELADO
     * @throws PagoException si el pago no está en estado PENDIENTE
     */
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

    /**
     * Retorna todos los pagos de un evento con sus relaciones resueltas.
     *
     * @param eventoId ID del evento
     * @return lista de pagos con datos de evento, cliente, estado, concepto y método
     */
    @Override
    @Transactional(readOnly = true)
    public List<PagoResponseDTO> obtenerPorEvento(Long eventoId) {
        return pagoRepository.findByEventoIdWithRelations(eventoId)
                .stream().map(pagoMapper::toDTO).toList();
    }

    /**
     * Retorna un listado paginado de todos los pagos del sistema.
     *
     * @param pageable parámetros de paginación y ordenamiento
     * @return página de pagos con datos de evento y cliente
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PagoResponseDTO> obtenerTodos(Pageable pageable) {
        return pagoRepository.findAllWithEvento(pageable)
                .map(pagoMapper::toDTO);
    }

    /**
     * Obtiene un resumen completo de pagos para un evento.
     * <p>
     * Combina los datos agregados de la vista {@code vista_pagos_evento}
     * (total cobrado, saldo pendiente, devoluciones) con la información
     * de anticipo mínimo calculada desde {@link ConfiguracionService}.
     * Si el evento no tiene datos en la vista, devuelve valores en cero.
     *
     * @param eventoId ID del evento
     * @return resumen de pagos con totales e información de anticipo
     */
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

    /**
     * Verifica si el evento puede avanzar de PROGRAMADO a CONFIRMADO tras un pago.
     * <p>
     * Si el evento está en PROGRAMADO y el total pagado alcanza o supera el
     * umbral mínimo de anticipo (según {@link ConfiguracionService}), gatilla
     * la transición automática.
     * <p>
     * Es seguro llamarlo después de confirmar cualquier pago — retorna
     * inmediatamente si el evento no está en PROGRAMADO.
     *
     * @param evento el evento a evaluar (debe tener estado y precio cargados)
     */
    private void verificarAnticipo(Evento evento) {
        if (!evento.getEstadoEvento().getNombre().equals(EstadoEventoNames.PROGRAMADO)) {
            return;
        }

        if (eventoPagoValidator.tieneAnticipoSuficiente(evento)) {
            eventoService.confirmarDesdeAnticipo(evento.getId());
        }
    }

    /**
     * Calcula la información de anticipo mínimo para un evento.
     * <p>
     * Obtiene el porcentaje mínimo desde la configuración del sistema,
     * calcula el monto requerido, y compara con el total pagado y el
     * monto pagado específicamente bajo el concepto ANTICIPO.
     *
     * @param evento el evento a evaluar
     * @return DTO con todos los datos de anticipo (porcentaje, montos, cumplimiento)
     */
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

    /**
     * Retorna el listado de cobranza con eventos activos y su estado financiero.
     * <p>
     * Ejecuta una consulta nativa que combina eventos, clientes y la vista
     * {@code vista_pagos_evento}. Aplica filtros opcionales en memoria:
     * <ul>
     *   <li>{@code estado} — filtrar por estado(s) del evento</li>
     *   <li>{@code soloConSaldo} — solo eventos con saldo pendiente &gt; 0</li>
     *   <li>{@code soloConDevolucion} — solo eventos con devolución pendiente</li>
     *   <li>{@code soloConPagosPendientes} — solo eventos con pagos en PENDIENTE</li>
     *   <li>{@code q} — búsqueda textual por nombre de cliente o ID de evento</li>
     *   <li>{@code limit} — limite de resultados</li>
     * </ul>
     * Los resultados se ordenan por prioridad: primero los que tienen devolución,
     * luego los que deben saldo, luego los que tienen pagos pendientes.
     *
     * @param filtros mapa de filtros opcionales
     * @return lista de eventos con datos financieros, ordenados por prioridad
     */
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
