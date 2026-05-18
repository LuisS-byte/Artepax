package com.catolica.Artepax.inventario.decoracion.service;

import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.TipoEventoRepository;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionProductoRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionProducto;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionTipoEvento;
import com.catolica.Artepax.inventario.decoracion.entity.DecoracionTipoEventoId;
import com.catolica.Artepax.inventario.decoracion.mapper.DecoracionMapper;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionProductoRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionRepository;
import com.catolica.Artepax.inventario.decoracion.repository.DecoracionTipoEventoRepository;
import com.catolica.Artepax.inventario.decoracion.service.impl.DecoracionServiceImpl;
import com.catolica.Artepax.inventario.producto.entity.Producto;
import com.catolica.Artepax.inventario.producto.entity.StockProducto;
import com.catolica.Artepax.inventario.producto.repository.ProductoRepository;
import com.catolica.Artepax.inventario.producto.repository.StockProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DecoracionServiceImpl} focusing on the new
 * decoracion-tipoevento compatibility logic.
 */
@ExtendWith(MockitoExtension.class)
class DecoracionServiceImplTest {

    @Mock
    private DecoracionRepository decoracionRepository;
    @Mock
    private DecoracionProductoRepository decoracionProductoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private StockProductoRepository stockProductoRepository;
    @Mock
    private DecoracionTipoEventoRepository decoracionTipoEventoRepository;
    @Mock
    private TipoEventoRepository tipoEventoRepository;

    private DecoracionMapper decoracionMapper = new DecoracionMapper();
    private DecoracionServiceImpl service;

    @Captor
    private ArgumentCaptor<List<DecoracionTipoEvento>> tipoEventoListCaptor;

    @BeforeEach
    void setUp() {
        service = new DecoracionServiceImpl(
                decoracionRepository,
                decoracionProductoRepository,
                productoRepository,
                stockProductoRepository,
                decoracionMapper,
                decoracionTipoEventoRepository,
                tipoEventoRepository);
    }

    // --- crearDecoracion con tipoEventoIds ---

    @Test
    void crearDecoracion_conTiposValidos_guardaCompatibilidades() {
        DecoracionRequestDTO dto = crearRequestConTipos(List.of(1L, 2L));
        Decoracion guardada = crearDecoracionGuardada(1L);
        Producto producto = crearProductoActivo(10L, "Mesa");
        StockProducto stock = crearStockDisponible(producto);

        when(decoracionRepository.findByNombreIgnoreCase(dto.getNombre())).thenReturn(Optional.empty());
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(stockProductoRepository.findAllByIdProductoIn(anyList())).thenReturn(List.of(stock));
        when(tipoEventoRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                crearTipoEvento(1L, "BODA", true),
                crearTipoEvento(2L, "QUINCE", true)));
        when(decoracionRepository.save(any(Decoracion.class))).thenReturn(guardada);

        DecoracionResponseDTO response = service.crearDecoracion(dto);

        assertNotNull(response);
        verify(decoracionTipoEventoRepository).saveAll(tipoEventoListCaptor.capture());
        List<DecoracionTipoEvento> saved = tipoEventoListCaptor.getValue();
        assertEquals(2, saved.size());
        assertEquals(1L, saved.get(0).getTipoEvento().getId());
        assertEquals(2L, saved.get(1).getTipoEvento().getId());
    }

    @Test
    void crearDecoracion_sinTipos_lanzaError() {
        DecoracionRequestDTO dto = crearRequestConTipos(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crearDecoracion(dto));
        assertTrue(ex.getMessage().contains("Debe seleccionar al menos un tipo"));
        verify(decoracionRepository, never()).save(any());
    }

    @Test
    void crearDecoracion_conTiposVacios_lanzaError() {
        DecoracionRequestDTO dto = crearRequestConTipos(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crearDecoracion(dto));
        assertTrue(ex.getMessage().contains("Debe seleccionar al menos un tipo"));
        verify(decoracionRepository, never()).save(any());
    }

    @Test
    void crearDecoracion_conTipoInactivo_lanzaError() {
        DecoracionRequestDTO dto = crearRequestConTipos(List.of(1L));
        Decoracion guardada = crearDecoracionGuardada(1L);
        Producto producto = crearProductoActivo(10L, "Mesa");
        StockProducto stock = crearStockDisponible(producto);

        when(decoracionRepository.findByNombreIgnoreCase(dto.getNombre())).thenReturn(Optional.empty());
        when(tipoEventoRepository.findAllById(List.of(1L))).thenReturn(List.of(
                crearTipoEvento(1L, "BODA", false)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crearDecoracion(dto));
        assertTrue(ex.getMessage().contains("inactivos"));
        verify(decoracionRepository, never()).save(any());
    }

    @Test
    void crearDecoracion_conTipoInexistente_lanzaError() {
        DecoracionRequestDTO dto = crearRequestConTipos(List.of(1L, 999L));

        when(decoracionRepository.findByNombreIgnoreCase(dto.getNombre())).thenReturn(Optional.empty());
        when(tipoEventoRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(
                crearTipoEvento(1L, "BODA", true)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crearDecoracion(dto));
        assertTrue(ex.getMessage().contains("no existen"));
        verify(decoracionRepository, never()).save(any());
    }

    // --- actualizar con tipoEventoIds ---

    @Test
    void actualizar_conTiposValidos_reemplazaCompatibilidades() {
        Decoracion existente = crearDecoracionGuardada(1L);
        DecoracionUpdateDTO dto = new DecoracionUpdateDTO();
        dto.setNombre("Actualizada");
        dto.setTipoEventoIds(List.of(3L));

        when(decoracionRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(decoracionRepository.findByNombreIgnoreCase("Actualizada")).thenReturn(Optional.empty());
        when(tipoEventoRepository.findAllById(List.of(3L))).thenReturn(List.of(
                crearTipoEvento(3L, "EMPRESARIAL", true)));

        service.actualizar(1L, dto);

        verify(decoracionTipoEventoRepository).deleteByDecoracion_Id(1L);
        verify(decoracionTipoEventoRepository).saveAll(tipoEventoListCaptor.capture());
        List<DecoracionTipoEvento> saved = tipoEventoListCaptor.getValue();
        assertEquals(1, saved.size());
        assertEquals(3L, saved.get(0).getTipoEvento().getId());
    }

    @Test
    void actualizar_sinTipos_conservaCompatibilidades() {
        Decoracion existente = crearDecoracionGuardada(1L);
        DecoracionUpdateDTO dto = new DecoracionUpdateDTO();
        dto.setNombre("Actualizada");
        dto.setTipoEventoIds(null);

        when(decoracionRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(decoracionRepository.findByNombreIgnoreCase("Actualizada")).thenReturn(Optional.empty());

        service.actualizar(1L, dto);

        verify(decoracionTipoEventoRepository, never()).deleteByDecoracion_Id(any());
        verify(decoracionTipoEventoRepository, never()).saveAll(anyList());
    }

    // --- obtenerCompatiblesPorTipoEvento ---

    @Test
    void obtenerCompatiblesPorTipoEvento_conTipoValido_retornaDecoraciones() {
        TipoEvento tipo = crearTipoEvento(3L, "BODA", true);
        Decoracion deco1 = crearDecoracionGuardada(1L);

        when(tipoEventoRepository.findById(3L)).thenReturn(Optional.of(tipo));
        when(decoracionTipoEventoRepository.findDecoracionesActivasCompatibles(3L))
                .thenReturn(List.of(deco1));
        when(decoracionProductoRepository.findByDecoracionIdWithProducto(1L))
                .thenReturn(List.of());

        List<DecoracionResponseDTO> result = service.obtenerCompatiblesPorTipoEvento(3L);

        assertEquals(1, result.size());
        verify(tipoEventoRepository).findById(3L);
    }

    @Test
    void obtenerCompatiblesPorTipoEvento_conTipoInexistente_lanzaError() {
        when(tipoEventoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.obtenerCompatiblesPorTipoEvento(999L));
    }

    @Test
    void obtenerCompatiblesPorTipoEvento_conTipoInactivo_lanzaError() {
        TipoEvento tipo = crearTipoEvento(1L, "BODA", false);
        when(tipoEventoRepository.findById(1L)).thenReturn(Optional.of(tipo));

        assertThrows(IllegalArgumentException.class,
                () -> service.obtenerCompatiblesPorTipoEvento(1L));
    }

    // --- helpers ---

    private DecoracionRequestDTO crearRequestConTipos(List<Long> tipoEventoIds) {
        DecoracionRequestDTO dto = new DecoracionRequestDTO();
        dto.setNombre("Decoracion Test");
        dto.setPrecio(new BigDecimal("1000.00"));
        dto.setTipoEventoIds(tipoEventoIds);

        DecoracionProductoRequestDTO prod = new DecoracionProductoRequestDTO();
        prod.setProductoId(10L);
        prod.setCantidadRequerida(2);
        dto.setProductos(List.of(prod));
        return dto;
    }

    private Decoracion crearDecoracionGuardada(Long id) {
        Decoracion d = new Decoracion();
        try {
            var idField = Decoracion.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(d, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        d.setNombre("Decoracion " + id);
        d.setPrecio(new BigDecimal("1000.00"));
        d.setEstado("ACTIVO");
        return d;
    }

    private Producto crearProductoActivo(Long id, String nombre) {
        Producto p = new Producto();
        try {
            var idField = Producto.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        p.setNombre(nombre);
        p.setEstado("ACTIVO");
        return p;
    }

    private StockProducto crearStockDisponible(Producto producto) {
        StockProducto stock = new StockProducto();
        try {
            var idField = StockProducto.class.getDeclaredField("idProducto");
            idField.setAccessible(true);
            idField.set(stock, producto.getId());
            var nombreField = StockProducto.class.getDeclaredField("nombre");
            nombreField.setAccessible(true);
            nombreField.set(stock, producto.getNombre());
            var dispField = StockProducto.class.getDeclaredField("cantidadDisponible");
            dispField.setAccessible(true);
            dispField.set(stock, 100);
            var alertaField = StockProducto.class.getDeclaredField("stockBajoAlerta");
            alertaField.setAccessible(true);
            alertaField.set(stock, false);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return stock;
    }

    private TipoEvento crearTipoEvento(Long id, String nombre, boolean activo) {
        TipoEvento tipo = new TipoEvento();
        tipo.setId(id);
        tipo.setTipoNombre(nombre);
        tipo.setTipoActivo(activo);
        return tipo;
    }
}
