package com.catolica.Artepax.evento.service.handler;

import com.catolica.Artepax.common.exception.ArtepaxException;
import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.clientes.repository.ClienteRepository;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.EstadoEventoNames;
import com.catolica.Artepax.evento.model.EstadoReserva;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.ReservaEstados;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.EstadoEventoRepository;
import com.catolica.Artepax.evento.repository.EstadoReservaRepository;
import com.catolica.Artepax.evento.repository.EventoProductoRepository;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.ReservaStockProducto;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.evento.service.EventoService;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProductoId;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class EventoStockConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("artepax_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EventoService eventoService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private EventoProductoRepository eventoProductoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DecoracionRepository decoracionRepository;

    @Autowired
    private DecoracionProductoRepository decoracionProductoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    @Autowired
    private EstadoEventoRepository estadoEventoRepository;

    @Autowired
    private EstadoReservaRepository estadoReservaRepository;

    @BeforeEach
    void setUp() {
        eventoProductoRepository.deleteAll();
        eventoRepository.deleteAll();
        decoracionProductoRepository.deleteAll();
        decoracionRepository.deleteAll();
        productoRepository.deleteAll();
        clienteRepository.deleteAll();
        tipoEventoRepository.deleteAll();
        estadoEventoRepository.deleteAll();
        estadoReservaRepository.deleteAll();
    }

    @Test
    void dosEventosReservanUltimoStock_soloUnoPasa() throws Exception {
        EstadoEvento cotizacion = guardarEstadoEvento(EstadoEventoNames.COTIZACION);
        guardarEstadoEvento(EstadoEventoNames.PROGRAMADO);
        guardarEstadoEvento(EstadoEventoNames.CONFIRMADO);
        guardarEstadoEvento(EstadoEventoNames.EN_MONTAJE);
        guardarEstadoEvento(EstadoEventoNames.CANCELACION_PENDIENTE);

        guardarEstadoReserva(ReservaEstados.ACTIVA);
        guardarEstadoReserva(ReservaEstados.LIBERADA);
        guardarEstadoReserva(ReservaEstados.CONSUMIDA);

        Cliente cliente = clienteRepository.save(crearCliente());
        TipoEvento tipoEvento = tipoEventoRepository.save(crearTipoEvento());
        Producto producto = productoRepository.save(crearProducto());
        Decoracion decoracion = decoracionRepository.save(crearDecoracion());
        decoracionProductoRepository.save(crearDecoracionProducto(decoracion, producto));

        Evento eventoA = eventoRepository.save(crearEvento(cliente, decoracion, tipoEvento, cotizacion, "Salon A"));
        Evento eventoB = eventoRepository.save(crearEvento(cliente, decoracion, tipoEvento, cotizacion, "Salon B"));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger errores = new AtomicInteger();
        Queue<Throwable> fallos = new ConcurrentLinkedQueue<>();

        Thread hiloA = new Thread(crearTareaProgramacion(eventoA.getId(), start, done, exitos, errores, fallos), "programar-evento-a");
        Thread hiloB = new Thread(crearTareaProgramacion(eventoB.getId(), start, done, exitos, errores, fallos), "programar-evento-b");

        hiloA.start();
        hiloB.start();

        start.countDown();

        assertTrue(done.await(10, TimeUnit.SECONDS), "Las tareas concurrentes no terminaron a tiempo");

        long eventosProgramados = eventoRepository.countByEstadoEventoNombre(EstadoEventoNames.PROGRAMADO);
        long eventosNoProgramados = eventoRepository.count() - eventosProgramados;
        long reservasActivasProducto = eventoProductoRepository
                .countByProductoIdAndEstadoReservaEstadoNombre(producto.getId(), ReservaEstados.ACTIVA);
        int cantidadReservadaActiva = eventoProductoRepository
                .sumCantidadReservadaByProductoIdsAndEstadoReservaNombre(List.of(producto.getId()), ReservaEstados.ACTIVA)
                .stream()
                .findFirst()
                .map(ReservaStockProducto::getCantidadReservada)
                .orElse(0);

        assertEquals(1, exitos.get(), "exitos");
        assertEquals(1, errores.get(), "errores");
        assertEquals(1, fallos.size(), "cantidad de fallos capturados");
        assertTrue(fallos.peek() instanceof ArtepaxException, "el fallo esperado debe ser una excepción de negocio");
        assertEquals(1L, reservasActivasProducto, "reservas ACTIVA del producto");
        assertEquals(1, cantidadReservadaActiva, "cantidad total reservada ACTIVA");
        assertEquals(1L, eventosProgramados, "eventos PROGRAMADO");
        assertEquals(1L, eventosNoProgramados, "eventos no programados");
    }

    private Runnable crearTareaProgramacion(Long eventoId,
                                            CountDownLatch start,
                                            CountDownLatch done,
                                            AtomicInteger exitos,
                                            AtomicInteger errores,
                                            Queue<Throwable> fallos) {
        return () -> {
            try {
                assertTrue(start.await(10, TimeUnit.SECONDS), "No se liberó el inicio concurrente");
                eventoService.programarEvento(eventoId);
                exitos.incrementAndGet();
            } catch (Throwable ex) {
                errores.incrementAndGet();
                fallos.add(ex);
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                done.countDown();
            }
        };
    }

    private EstadoEvento guardarEstadoEvento(String nombre) {
        EstadoEvento estado = new EstadoEvento();
        estado.setNombre(nombre);
        estado.setDescripcion(nombre);
        return estadoEventoRepository.save(estado);
    }

    private EstadoReserva guardarEstadoReserva(String nombre) {
        EstadoReserva estado = new EstadoReserva();
        estado.setEstadoNombre(nombre);
        estado.setEstadoDescripcion(nombre);
        return estadoReservaRepository.save(estado);
    }

    private Cliente crearCliente() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Test");
        cliente.setCorreo("cliente@test.com");
        cliente.setTelefono("555-0101");
        cliente.setDireccion("Direccion Test");
        cliente.setActivo(true);
        return cliente;
    }

    private TipoEvento crearTipoEvento() {
        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setTipoNombre("Cumple");
        tipoEvento.setTipoDescripcion("Cumple test");
        tipoEvento.setTipoActivo(true);
        return tipoEvento;
    }

    private Producto crearProducto() {
        Producto producto = new Producto();
        producto.setNombre("Producto unico");
        producto.setDescripcion("Stock concurrente");
        producto.setCantidadTotal(1);
        producto.setPrecioUnitario(new BigDecimal("10.00"));
        producto.setStockMinimo(0);
        producto.setEstado("ACTIVO");
        return producto;
    }

    private Decoracion crearDecoracion() {
        Decoracion decoracion = new Decoracion();
        decoracion.setNombre("Decoracion concurrencia");
        decoracion.setDescripcion("Decoracion test");
        decoracion.setPrecio(new BigDecimal("100.00"));
        decoracion.setEstado("ACTIVO");
        return decoracion;
    }

    private DecoracionProducto crearDecoracionProducto(Decoracion decoracion, Producto producto) {
        DecoracionProducto relacion = new DecoracionProducto();
        DecoracionProductoId id = new DecoracionProductoId();
        id.setDecoracionId(decoracion.getId());
        id.setProductoId(producto.getId());
        relacion.setId(id);
        relacion.setDecoracion(decoracion);
        relacion.setProducto(producto);
        relacion.setCantidadRequerida(1);
        return relacion;
    }

    private Evento crearEvento(Cliente cliente,
                               Decoracion decoracion,
                               TipoEvento tipoEvento,
                               EstadoEvento estadoEvento,
                               String ubicacion) {
        Evento evento = new Evento();
        evento.setCliente(cliente);
        evento.setDecoracion(decoracion);
        evento.setTipoEvento(tipoEvento);
        evento.setEstadoEvento(estadoEvento);
        evento.setFecha(LocalDate.now().plusDays(10));
        evento.setHoraInicio(LocalTime.of(10, 0));
        evento.setHoraFin(LocalTime.of(12, 0));
        evento.setUbicacion(ubicacion);
        evento.setPrecioBase(new BigDecimal("100.00"));
        evento.setPrecioAdicional(BigDecimal.ZERO);
        evento.setClienteNotificado(false);
        evento.setFechaExpiracion(LocalDate.now().plusDays(20));
        return evento;
    }
}
