package com.catolica.Artepax.evento.service.handler;

import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.model.EstadoReserva;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.EventoProducto;
import com.catolica.Artepax.evento.model.EventoProductoId;
import com.catolica.Artepax.evento.model.ReservaEstados;
import com.catolica.Artepax.evento.repository.EstadoReservaRepository;
import com.catolica.Artepax.evento.repository.EventoProductoRepository;
import com.catolica.Artepax.evento.repository.ReservaStockProducto;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.exception.StockInsuficienteException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handler encargado de la gestión de stock para eventos.
 * <p>
 * Centraliza la validación de disponibilidad, reserva de productos al programar
 * un evento, y consumo de stock al finalizarlo.
 */
@Component
public class EventoStockHandler {

    private static final Logger log = LoggerFactory.getLogger(EventoStockHandler.class);

    private final DecoracionProductoRepository decoracionProductoRepository;
    private final ProductoRepository productoRepository;
    private final EventoProductoRepository eventoProductoRepository;
    private final EstadoReservaRepository estadoReservaRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public EventoStockHandler(DecoracionProductoRepository decoracionProductoRepository,
                              ProductoRepository productoRepository,
                              EventoProductoRepository eventoProductoRepository,
                              EstadoReservaRepository estadoReservaRepository,
                              MovimientoInventarioRepository movimientoRepository) {
        this.decoracionProductoRepository = decoracionProductoRepository;
        this.productoRepository = productoRepository;
        this.eventoProductoRepository = eventoProductoRepository;
        this.estadoReservaRepository = estadoReservaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    /**
     * Valida que haya stock suficiente para todos los productos de la decoración
     * y crea los registros en evento_producto reservando las cantidades.
     *
     * @param evento evento a programar
     * @throws StockInsuficienteException si algún producto no tiene stock disponible
     */
    @Transactional
    public void validarYReservarStock(Evento evento) {
        List<DecoracionProducto> productosRequeridos =
                decoracionProductoRepository.findByDecoracionId(evento.getDecoracion().getId());

        Map<Long, Integer> cantidadesPorProductoId = productosRequeridos.stream()
                .collect(Collectors.toMap(
                        dp -> dp.getProducto().getId(),
                        DecoracionProducto::getCantidadRequerida,
                        Integer::sum,
                        LinkedHashMap::new));

        reservarStockActivo(evento, cantidadesPorProductoId);

        log.info("stock_reservado eventoId={} productos={}", evento.getId(), productosRequeridos.size());
    }

    /**
     * Consume el stock reservado al finalizar un evento:
     * reduce cantidad_total de cada producto y registra movimientos de salida.
     *
     * @param evento evento finalizado
     */
    @Transactional
    public void consumirStock(Evento evento) {
        EstadoReserva estadoConsumida = getEstadoReservaOrThrow(ReservaEstados.CONSUMIDA);
        List<EventoProducto> reservas = eventoProductoRepository
                .findByEventoIdAndEstadoReservaNombre(evento.getId(), ReservaEstados.ACTIVA);

        for (EventoProducto reserva : reservas) {
            Producto producto = reserva.getProducto();

            producto.setCantidadTotal(
                    producto.getCantidadTotal() - reserva.getCantidadReservada());
            productoRepository.save(producto);

            registrarMovimientoEvento(
                    producto,
                    evento,
                    reserva.getCantidadReservada(),
                    "Salida por evento finalizado ID: " + evento.getId());

            reserva.setEstadoReserva(estadoConsumida);
            reserva.setFechaLiberacion(LocalDateTime.now());
            reserva.setMotivoLiberacion("Reserva consumida por finalización del evento");
            eventoProductoRepository.save(reserva);
        }

        log.info("stock_consumido eventoId={} productos={}", evento.getId(), reservas.size());
    }

    @Transactional
    public void liberarReservas(Evento evento, String motivoLiberacion) {
        EstadoReserva estadoLiberada = getEstadoReservaOrThrow(ReservaEstados.LIBERADA);
        List<EventoProducto> reservas = eventoProductoRepository
                .findByEventoIdAndEstadoReservaNombre(evento.getId(), ReservaEstados.ACTIVA);

        for (EventoProducto reserva : reservas) {
            reserva.setEstadoReserva(estadoLiberada);
            reserva.setFechaLiberacion(LocalDateTime.now());
            reserva.setMotivoLiberacion(motivoLiberacion);
            eventoProductoRepository.save(reserva);
        }

        log.info("stock_liberado eventoId={} reservas={}", evento.getId(), reservas.size());
    }

    @Transactional
    public void validarYReservarExtras(Evento evento, Map<Producto, Integer> productosExtra) {
        Map<Long, Integer> cantidadesPorProductoId = productosExtra.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getId(),
                        Map.Entry::getValue,
                        Integer::sum,
                        LinkedHashMap::new));

        reservarStockActivo(evento, cantidadesPorProductoId);

        log.info("stock_extra_reservado eventoId={} productos={}", evento.getId(), productosExtra.size());
    }

    private void reservarStockActivo(Evento evento, Map<Long, Integer> cantidadesPorProductoId) {
        if (cantidadesPorProductoId.isEmpty()) {
            return;
        }

        EstadoReserva estadoActiva = getEstadoReservaOrThrow(ReservaEstados.ACTIVA);
        List<Long> productoIdsOrdenados = cantidadesPorProductoId.keySet().stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        Map<Long, Producto> productosBloqueados = productoRepository.findAllByIdInForUpdate(productoIdsOrdenados)
                .stream()
                .collect(Collectors.toMap(Producto::getId, producto -> producto));

        validarProductosReservables(productoIdsOrdenados, productosBloqueados);

        Map<Long, Integer> cantidadesReservadasActivas = eventoProductoRepository
                .sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(productoIdsOrdenados, ReservaEstados.ACTIVA)
                .stream()
                .collect(Collectors.toMap(
                        ReservaStockProducto::getProductoId,
                        reserva -> safeInt(reserva.getCantidadReservada())));

        List<EventoProducto> reservasActivasEvento = eventoProductoRepository
                .findByEventoIdAndProductoIdsAndEstadoReservaNombre(
                        evento.getId(), productoIdsOrdenados, ReservaEstados.ACTIVA);

        Map<Long, EventoProducto> reservasActivasPorProducto = reservasActivasEvento.stream()
                .collect(Collectors.toMap(reserva -> reserva.getProducto().getId(), reserva -> reserva));

        for (Long productoId : productoIdsOrdenados) {
            Producto producto = productosBloqueados.get(productoId);
            int cantidadSolicitada = safeInt(cantidadesPorProductoId.get(productoId));
            int cantidadDisponible = safeInt(producto.getCantidadTotal())
                    - cantidadesReservadasActivas.getOrDefault(productoId, 0);

            if (cantidadDisponible < cantidadSolicitada) {
                throw new StockInsuficienteException(
                        producto.getNombre(),
                        cantidadDisponible,
                        cantidadSolicitada);
            }

            EventoProducto reserva = reservasActivasPorProducto.get(productoId);
            if (reserva == null) {
                reserva = new EventoProducto();
                reserva.setId(new EventoProductoId(evento.getId(), productoId));
                reserva.setEvento(evento);
                reserva.setProducto(producto);
                reserva.setCantidadReservada(cantidadSolicitada);
                reserva.setEstadoReserva(estadoActiva);
                reserva.setFechaReserva(LocalDateTime.now());
            } else {
                reserva.setCantidadReservada(reserva.getCantidadReservada() + cantidadSolicitada);
            }

            eventoProductoRepository.save(reserva);
        }
    }

    private void validarProductosReservables(List<Long> productoIdsOrdenados,
                                             Map<Long, Producto> productosBloqueados) {
        for (Long productoId : productoIdsOrdenados) {
            Producto producto = productosBloqueados.get(productoId);
            if (producto == null) {
                throw new EventoException("Producto no encontrado con ID: " + productoId);
            }
            if (!"ACTIVO".equals(producto.getEstado())) {
                throw new EventoException("Producto no encontrado o inactivo");
            }
        }
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private void registrarMovimientoEvento(Producto producto, Evento evento,
                                           int cantidad, String motivo) {
        MovimientoInventario m = new MovimientoInventario();
        m.setProducto(producto);
        m.setTipoMovimiento("SALIDA");
        m.setTipoOrigen("EVENTO");
        m.setEvento(evento);
        m.setCantidad(cantidad);
        m.setMotivo(motivo);
        movimientoRepository.save(m);
    }

    private EstadoReserva getEstadoReservaOrThrow(String estadoNombre) {
        return estadoReservaRepository.findByEstadoNombre(estadoNombre)
                .orElseThrow(() -> new EventoException(
                        "Estado de reserva no encontrado en BD: " + estadoNombre));
    }
}
