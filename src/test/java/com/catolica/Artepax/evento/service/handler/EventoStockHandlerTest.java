package com.catolica.Artepax.evento.service.handler;

import com.catolica.Artepax.evento.model.EstadoReserva;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.EventoProducto;
import com.catolica.Artepax.evento.model.EventoProductoId;
import com.catolica.Artepax.evento.model.ReservaEstados;
import com.catolica.Artepax.evento.repository.EstadoReservaRepository;
import com.catolica.Artepax.evento.repository.EventoProductoRepository;
import com.catolica.Artepax.evento.repository.ReservaStockProducto;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.exception.StockInsuficienteException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoStockHandlerTest {

    @Mock
    private DecoracionProductoRepository decoracionProductoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private EventoProductoRepository eventoProductoRepository;
    @Mock
    private EstadoReservaRepository estadoReservaRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    private EventoStockHandler handler;

    @BeforeEach
    void setUp() {
        handler = new EventoStockHandler(
                decoracionProductoRepository,
                productoRepository,
                eventoProductoRepository,
                estadoReservaRepository,
                movimientoRepository);
    }

    @Test
    void validarYReservarStock_conStockSuficiente_creaReservaActiva() {
        Evento evento = crearEvento(10L);
        evento.setDecoracion(crearDecoracion(5L));
        Producto producto = crearProducto(7L, "Luces", "ACTIVO", new BigDecimal("10.00"), 5);
        var decoracionProducto = crearDecoracionProducto(producto, 2);
        EstadoReserva estadoActiva = crearEstadoReserva(ReservaEstados.ACTIVA);

        when(decoracionProductoRepository.findByDecoracionId(5L)).thenReturn(List.of(decoracionProducto));
        when(productoRepository.findAllByIdInForUpdate(List.of(7L))).thenReturn(List.of(producto));
        when(estadoReservaRepository.findByEstadoNombre(ReservaEstados.ACTIVA)).thenReturn(Optional.of(estadoActiva));
        when(eventoProductoRepository.sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());
        when(eventoProductoRepository.findByEventoIdAndProductoIdsAndEstadoReservaNombre(10L, List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());
        when(eventoProductoRepository.save(any(EventoProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.validarYReservarStock(evento);

        ArgumentCaptor<EventoProducto> captor = ArgumentCaptor.forClass(EventoProducto.class);
        verify(eventoProductoRepository).save(captor.capture());
        EventoProducto guardada = captor.getValue();
        assertEquals(ReservaEstados.ACTIVA, guardada.getEstadoReserva().getEstadoNombre());
        assertEquals(2, guardada.getCantidadReservada());
        assertEquals(10L, guardada.getEvento().getId());
        assertEquals(7L, guardada.getProducto().getId());
        assertEquals(true, guardada.getFechaReserva() != null);
    }

    @Test
    void validarYReservarStock_sinStockSuficiente_lanzaError() {
        Evento evento = crearEvento(10L);
        evento.setDecoracion(crearDecoracion(5L));
        Producto producto = crearProducto(7L, "Luces", "ACTIVO", new BigDecimal("10.00"), 1);
        var decoracionProducto = crearDecoracionProducto(producto, 2);

        when(decoracionProductoRepository.findByDecoracionId(5L)).thenReturn(List.of(decoracionProducto));
        when(productoRepository.findAllByIdInForUpdate(List.of(7L))).thenReturn(List.of(producto));
        when(estadoReservaRepository.findByEstadoNombre(ReservaEstados.ACTIVA))
                .thenReturn(Optional.of(crearEstadoReserva(ReservaEstados.ACTIVA)));
        when(eventoProductoRepository.sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of(reservaStock(7L, 1)));
        when(eventoProductoRepository.findByEventoIdAndProductoIdsAndEstadoReservaNombre(10L, List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());

        assertThrows(StockInsuficienteException.class, () -> handler.validarYReservarStock(evento));

        verify(eventoProductoRepository, never()).save(any(EventoProducto.class));
    }

    @Test
    void agregarExtras_conStockSuficiente_incrementaReservaActiva() {
        Evento evento = crearEvento(10L);
        Producto producto = crearProducto(7L, "Luces", "ACTIVO", new BigDecimal("10.00"), 5);
        EventoProducto reservaExistente = new EventoProducto();
        reservaExistente.setId(new EventoProductoId(10L, 7L));
        reservaExistente.setEvento(evento);
        reservaExistente.setProducto(producto);
        reservaExistente.setCantidadReservada(3);
        reservaExistente.setEstadoReserva(crearEstadoReserva(ReservaEstados.ACTIVA));
        reservaExistente.setFechaReserva(LocalDateTime.now().minusDays(1));

        Map<Producto, Integer> extras = new LinkedHashMap<>();
        extras.put(producto, 2);
        when(productoRepository.findAllByIdInForUpdate(List.of(7L))).thenReturn(List.of(producto));
        when(estadoReservaRepository.findByEstadoNombre(ReservaEstados.ACTIVA))
                .thenReturn(Optional.of(crearEstadoReserva(ReservaEstados.ACTIVA)));
        when(eventoProductoRepository.sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of(reservaStock(7L, 3)));
        when(eventoProductoRepository.findByEventoIdAndProductoIdsAndEstadoReservaNombre(10L, List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of(reservaExistente));
        when(eventoProductoRepository.save(any(EventoProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.validarYReservarExtras(evento, extras);

        assertEquals(5, reservaExistente.getCantidadReservada());
        verify(eventoProductoRepository).save(reservaExistente);
    }

    @Test
    void validarYReservarStock_noIncrementaReservasLiberadas() {
        Evento evento = crearEvento(10L);
        Producto producto = crearProducto(7L, "Luces", "ACTIVO", new BigDecimal("10.00"), 5);
        EventoProducto reservaLiberada = new EventoProducto();
        reservaLiberada.setId(new EventoProductoId(10L, 7L));
        reservaLiberada.setEvento(evento);
        reservaLiberada.setProducto(producto);
        reservaLiberada.setCantidadReservada(3);
        reservaLiberada.setEstadoReserva(crearEstadoReserva(ReservaEstados.LIBERADA));
        reservaLiberada.setFechaReserva(LocalDateTime.now().minusDays(2));
        reservaLiberada.setFechaLiberacion(LocalDateTime.now().minusDays(1));

        Map<Producto, Integer> extras = new LinkedHashMap<>();
        extras.put(producto, 2);

        when(productoRepository.findAllByIdInForUpdate(List.of(7L))).thenReturn(List.of(producto));
        when(estadoReservaRepository.findByEstadoNombre(ReservaEstados.ACTIVA))
                .thenReturn(Optional.of(crearEstadoReserva(ReservaEstados.ACTIVA)));
        when(eventoProductoRepository.sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());
        when(eventoProductoRepository.findByEventoIdAndProductoIdsAndEstadoReservaNombre(10L, List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());
        when(eventoProductoRepository.save(any(EventoProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.validarYReservarExtras(evento, extras);

        assertEquals(3, reservaLiberada.getCantidadReservada());
        verify(eventoProductoRepository).save(any(EventoProducto.class));
    }

    @Test
    void validarYReservarStock_noIncrementaReservasConsumidas() {
        Evento evento = crearEvento(10L);
        Producto producto = crearProducto(7L, "Luces", "ACTIVO", new BigDecimal("10.00"), 5);
        EventoProducto reservaConsumida = new EventoProducto();
        reservaConsumida.setId(new EventoProductoId(10L, 7L));
        reservaConsumida.setEvento(evento);
        reservaConsumida.setProducto(producto);
        reservaConsumida.setCantidadReservada(3);
        reservaConsumida.setEstadoReserva(crearEstadoReserva(ReservaEstados.CONSUMIDA));
        reservaConsumida.setFechaReserva(LocalDateTime.now().minusDays(2));
        reservaConsumida.setFechaLiberacion(LocalDateTime.now().minusDays(1));

        Map<Producto, Integer> extras = new LinkedHashMap<>();
        extras.put(producto, 2);

        when(productoRepository.findAllByIdInForUpdate(List.of(7L))).thenReturn(List.of(producto));
        when(estadoReservaRepository.findByEstadoNombre(ReservaEstados.ACTIVA))
                .thenReturn(Optional.of(crearEstadoReserva(ReservaEstados.ACTIVA)));
        when(eventoProductoRepository.sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());
        when(eventoProductoRepository.findByEventoIdAndProductoIdsAndEstadoReservaNombre(10L, List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());
        when(eventoProductoRepository.save(any(EventoProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.validarYReservarExtras(evento, extras);

        assertEquals(3, reservaConsumida.getCantidadReservada());
        verify(eventoProductoRepository).save(any(EventoProducto.class));
    }

    @Test
    void agregarExtras_sinStockSuficiente_lanzaError() {
        Evento evento = crearEvento(10L);
        Producto producto = crearProducto(7L, "Luces", "ACTIVO", new BigDecimal("10.00"), 3);
        Map<Producto, Integer> extras = new LinkedHashMap<>();
        extras.put(producto, 4);

        when(productoRepository.findAllByIdInForUpdate(List.of(7L))).thenReturn(List.of(producto));
        when(estadoReservaRepository.findByEstadoNombre(ReservaEstados.ACTIVA))
                .thenReturn(Optional.of(crearEstadoReserva(ReservaEstados.ACTIVA)));
        when(eventoProductoRepository.sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());
        when(eventoProductoRepository.findByEventoIdAndProductoIdsAndEstadoReservaNombre(10L, List.of(7L), ReservaEstados.ACTIVA))
                .thenReturn(List.of());

        assertThrows(StockInsuficienteException.class, () -> handler.validarYReservarExtras(evento, extras));

        verify(eventoProductoRepository, never()).save(any(EventoProducto.class));
    }

    @Test
    void liberarReservasActivasLasMarcaComoLiberadas() {
        Evento evento = crearEvento(10L);
        EventoProducto reservaActiva = new EventoProducto();
        reservaActiva.setId(new EventoProductoId(10L, 7L));
        reservaActiva.setEvento(evento);
        reservaActiva.setProducto(crearProducto(7L, "Luces", "ACTIVO", new BigDecimal("10.00"), 5));
        reservaActiva.setCantidadReservada(3);
        reservaActiva.setEstadoReserva(crearEstadoReserva(ReservaEstados.ACTIVA));
        reservaActiva.setFechaReserva(LocalDateTime.now().minusDays(1));

        when(estadoReservaRepository.findByEstadoNombre(ReservaEstados.LIBERADA))
                .thenReturn(Optional.of(crearEstadoReserva(ReservaEstados.LIBERADA)));
        when(eventoProductoRepository.findByEventoIdAndEstadoReservaNombre(10L, ReservaEstados.ACTIVA))
                .thenReturn(List.of(reservaActiva));
        when(eventoProductoRepository.save(any(EventoProducto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.liberarReservas(evento, "Cliente canceló");

        assertEquals(ReservaEstados.LIBERADA, reservaActiva.getEstadoReserva().getEstadoNombre());
        assertEquals("Cliente canceló", reservaActiva.getMotivoLiberacion());
        assertEquals(true, reservaActiva.getFechaLiberacion() != null);
        verify(eventoProductoRepository).save(reservaActiva);
    }

    private EstadoReserva crearEstadoReserva(String nombre) {
        EstadoReserva estadoReserva = new EstadoReserva();
        estadoReserva.setEstadoNombre(nombre);
        return estadoReserva;
    }

    private Evento crearEvento(Long id) {
        Evento evento = new Evento();
        evento.setId(id);
        return evento;
    }

    private Producto crearProducto(Long id, String nombre, String estado, BigDecimal precioUnitario, Integer cantidadTotal) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setEstado(estado);
        producto.setPrecioUnitario(precioUnitario);
        producto.setCantidadTotal(cantidadTotal);

        try {
            var idField = Producto.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(producto, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        return producto;
    }

    private com.catolica.Artepax.inventario.decoracion.entity.Decoracion crearDecoracion(Long id) {
        var decoracion = new com.catolica.Artepax.inventario.decoracion.entity.Decoracion();
        try {
            setField(decoracion, "id", id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return decoracion;
    }

    private com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto crearDecoracionProducto(Producto producto,
                                                                                                          int cantidadRequerida) {
        var decoracionProducto = new com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto();
        decoracionProducto.setProducto(producto);
        decoracionProducto.setCantidadRequerida(cantidadRequerida);
        return decoracionProducto;
    }

    private ReservaStockProducto reservaStock(Long productoId, Integer cantidadReservada) {
        return new ReservaStockProducto() {
            @Override
            public Long getProductoId() {
                return productoId;
            }

            @Override
            public Integer getCantidadReservada() {
                return cantidadReservada;
            }
        };
    }

    private void setField(Object target, String fieldName, Object value)
            throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
