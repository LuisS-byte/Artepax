package com.catolica.Artepax.evento.service.handler;

import com.catolica.Artepax.evento.exception.EventoException;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.EventoProducto;
import com.catolica.Artepax.evento.model.EventoProductoId;
import com.catolica.Artepax.evento.repository.EventoProductoRepository;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.entity.StockProducto;
import com.catolica.Artepax.inventario.producto.exception.StockInsuficienteException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import com.catolica.Artepax.inventario.producto.repository.StockProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    private final StockProductoRepository stockProductoRepository;
    private final ProductoRepository productoRepository;
    private final EventoProductoRepository eventoProductoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public EventoStockHandler(DecoracionProductoRepository decoracionProductoRepository,
                              StockProductoRepository stockProductoRepository,
                              ProductoRepository productoRepository,
                              EventoProductoRepository eventoProductoRepository,
                              MovimientoInventarioRepository movimientoRepository) {
        this.decoracionProductoRepository = decoracionProductoRepository;
        this.stockProductoRepository = stockProductoRepository;
        this.productoRepository = productoRepository;
        this.eventoProductoRepository = eventoProductoRepository;
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

        List<Long> productoIds = productosRequeridos.stream()
                .map(dp -> dp.getProducto().getId())
                .toList();

        Map<Long, StockProducto> stockMap = stockProductoRepository
                .findAllByIdProductoIn(productoIds)
                .stream()
                .collect(Collectors.toMap(StockProducto::getIdProducto, s -> s));

        for (DecoracionProducto dp : productosRequeridos) {
            StockProducto stock = stockMap.get(dp.getProducto().getId());
            int disponible = stock != null ? stock.getCantidadDisponible() : 0;
            if (disponible < dp.getCantidadRequerida()) {
                throw new StockInsuficienteException(
                        dp.getProducto().getNombre(),
                        disponible,
                        dp.getCantidadRequerida());
            }
        }

        for (DecoracionProducto dp : productosRequeridos) {
            EventoProducto ep = new EventoProducto();
            ep.setId(new EventoProductoId(evento.getId(), dp.getProducto().getId()));
            ep.setEvento(evento);
            ep.setProducto(dp.getProducto());
            ep.setCantidadReservada(dp.getCantidadRequerida());
            eventoProductoRepository.save(ep);
        }

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
        List<EventoProducto> reservas = eventoProductoRepository.findByEventoId(evento.getId());

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
        }

        log.info("stock_consumido eventoId={} productos={}", evento.getId(), reservas.size());
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
}
