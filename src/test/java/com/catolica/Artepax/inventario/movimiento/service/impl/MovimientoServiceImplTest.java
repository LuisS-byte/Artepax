package com.catolica.Artepax.inventario.movimiento.service.impl;

import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.evento.repository.EventoProductoRepository;
import com.catolica.Artepax.inventario.movimiento.dto.AjusteInventarioRequestDTO;
import com.catolica.Artepax.inventario.movimiento.entity.MovimientoInventario;
import com.catolica.Artepax.inventario.movimiento.mapper.MovimientoMapper;
import com.catolica.Artepax.inventario.movimiento.repository.MovimientoInventarioRepository;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.exception.ProductoNotFoundException;
import com.catolica.Artepax.inventario.producto.exception.ReservaActivaInsuficienteException;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import com.catolica.Artepax.inventario.producto.repository.StockProductoRepository;
import com.catolica.Artepax.notificacion.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceImplTest {

    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private StockProductoRepository stockProductoRepository;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private EventoProductoRepository eventoProductoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private MovimientoServiceImpl movimientoService;

    @BeforeEach
    void setUp() {
        movimientoService = new MovimientoServiceImpl(
                movimientoRepository,
                productoRepository,
                stockProductoRepository,
                new MovimientoMapper(),
                notificacionService,
                null,
                eventoProductoRepository,
                auditoriaService);

        lenient().when(productoRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(movimientoRepository.save(any(MovimientoInventario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Producto crearProducto(String nombre, Integer cantidadTotal, String estado) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setCantidadTotal(cantidadTotal);
        p.setEstado(estado);
        p.setPrecioUnitario(BigDecimal.ZERO);
        p.setStockMinimo(0);
        return p;
    }

    @Test
    void ajustePositivo_creaEntrada() {
        Producto producto = crearProducto("Mantel", 10, "ACTIVO");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(eventoProductoRepository.sumCantidadReservadaActivaByProductoId(1L)).thenReturn(0);

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(15, "Conteo fisico");
        movimientoService.registrarAjuste(1L, request);

        assertEquals(15, producto.getCantidadTotal());
        verify(movimientoRepository).save(argThat(m ->
                "ENTRADA".equals(m.getTipoMovimiento())
                        && "AJUSTE_MANUAL".equals(m.getTipoOrigen())
                        && m.getCantidad() == 5
                        && "Conteo fisico".equals(m.getMotivo())));
    }

    @Test
    void ajusteNegativo_creaSalida() {
        Producto producto = crearProducto("Mantel", 10, "ACTIVO");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(eventoProductoRepository.sumCantidadReservadaActivaByProductoId(1L)).thenReturn(0);

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(7, "Mercancia danada");
        movimientoService.registrarAjuste(1L, request);

        assertEquals(7, producto.getCantidadTotal());
        verify(movimientoRepository).save(argThat(m ->
                "SALIDA".equals(m.getTipoMovimiento())
                        && "AJUSTE_MANUAL".equals(m.getTipoOrigen())
                        && m.getCantidad() == 3));
    }

    @Test
    void ajusteSinDiferencia_noCreaMovimiento() {
        Producto producto = crearProducto("Mantel", 10, "ACTIVO");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(eventoProductoRepository.sumCantidadReservadaActivaByProductoId(1L)).thenReturn(0);

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(10, "Sin cambios");
        movimientoService.registrarAjuste(1L, request);

        verify(productoRepository, never()).save(any());
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void productoInexistente_lanza404() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(10, "Test");
        assertThrows(ProductoNotFoundException.class,
                () -> movimientoService.registrarAjuste(99L, request));
    }

    @Test
    void productoInactivo_lanzaError() {
        Producto producto = crearProducto("Mantel viejo", 10, "INACTIVO");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(12, "Test");
        assertThrows(IllegalArgumentException.class,
                () -> movimientoService.registrarAjuste(1L, request));
    }

    @Test
    void ajusteBajoReservasActivas_lanzaError() {
        Producto producto = crearProducto("Mantel", 10, "ACTIVO");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(eventoProductoRepository.sumCantidadReservadaActivaByProductoId(1L)).thenReturn(8);

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(5, "Test");
        assertThrows(ReservaActivaInsuficienteException.class,
                () -> movimientoService.registrarAjuste(1L, request));
    }

    @Test
    void ajusteIgualAReservasActivas_permitido() {
        Producto producto = crearProducto("Mantel", 10, "ACTIVO");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(eventoProductoRepository.sumCantidadReservadaActivaByProductoId(1L)).thenReturn(8);

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(8, "Ajuste al limite");
        movimientoService.registrarAjuste(1L, request);

        assertEquals(8, producto.getCantidadTotal());
        verify(movimientoRepository).save(argThat(m ->
                "SALIDA".equals(m.getTipoMovimiento()) && m.getCantidad() == 2));
    }

    @Test
    void ajusteFallaMovimiento_noActualizaProducto() {
        Producto producto = crearProducto("Mantel", 10, "ACTIVO");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(eventoProductoRepository.sumCantidadReservadaActivaByProductoId(1L)).thenReturn(0);
        when(movimientoRepository.save(any(MovimientoInventario.class))).thenThrow(new RuntimeException("DB error"));

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(15, "Test");
        assertThrows(RuntimeException.class,
                () -> movimientoService.registrarAjuste(1L, request));
    }

    @Test
    void ajusteSinReservasActivas_ceroRetornado() {
        Producto producto = crearProducto("Mantel", 10, "ACTIVO");
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(eventoProductoRepository.sumCantidadReservadaActivaByProductoId(1L)).thenReturn(0);

        AjusteInventarioRequestDTO request = new AjusteInventarioRequestDTO(12, "Test");
        movimientoService.registrarAjuste(1L, request);

        assertEquals(12, producto.getCantidadTotal());
        verify(movimientoRepository).save(argThat(m ->
                "ENTRADA".equals(m.getTipoMovimiento()) && m.getCantidad() == 2));
    }
}
