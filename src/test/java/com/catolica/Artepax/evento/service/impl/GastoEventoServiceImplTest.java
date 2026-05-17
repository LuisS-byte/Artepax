package com.catolica.Artepax.evento.service.impl;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.evento.dto.EventoGastoControlDTO;
import com.catolica.Artepax.evento.dto.ResumenGastoEventoDTO;
import com.catolica.Artepax.evento.exception.EventoNotFoundException;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.GastoEvento;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.evento.repository.GastoEventoRepository;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GastoEventoServiceImplTest {

    @Mock
    private GastoEventoRepository gastoRepository;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private GastoEventoServiceImpl gastoService;

    @BeforeEach
    void setUp() {
        gastoService = new GastoEventoServiceImpl(gastoRepository, eventoRepository, auditoriaService);
    }

    private Evento crearEvento(Long id) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Demo");

        EstadoEvento estado = new EstadoEvento();
        estado.setNombre("PROGRAMADO");

        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setTipoNombre("BODA");

        Decoracion decoracion = new Decoracion();
        decoracion.setNombre("Clasica");

        Evento evento = new Evento();
        evento.setId(id);
        evento.setCliente(cliente);
        evento.setEstadoEvento(estado);
        evento.setTipoEvento(tipoEvento);
        evento.setDecoracion(decoracion);
        return evento;
    }

    @Test
    void eventoSinGastos_devuelveCeros() {
        when(eventoRepository.existsById(1L)).thenReturn(true);
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "PROYECTADO")).thenReturn(BigDecimal.ZERO);
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "DESEMBOLSADO")).thenReturn(BigDecimal.ZERO);

        ResumenGastoEventoDTO resumen = gastoService.getResumen(1L);

        assertEquals(BigDecimal.ZERO, resumen.totalProyectado());
        assertEquals(BigDecimal.ZERO, resumen.totalDesembolsado());
        assertEquals(BigDecimal.ZERO, resumen.totalPendiente());
    }

    @Test
    void soloProyectados_totalProyectadoYPendienteMayorACero() {
        when(eventoRepository.existsById(1L)).thenReturn(true);
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "PROYECTADO")).thenReturn(new BigDecimal("24.00"));
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "DESEMBOLSADO")).thenReturn(BigDecimal.ZERO);

        ResumenGastoEventoDTO resumen = gastoService.getResumen(1L);

        assertEquals(new BigDecimal("24.00"), resumen.totalProyectado());
        assertEquals(BigDecimal.ZERO, resumen.totalDesembolsado());
        assertEquals(new BigDecimal("24.00"), resumen.totalPendiente());
    }

    @Test
    void soloDesembolsados_totalDesembolsadoMayorACero() {
        when(eventoRepository.existsById(1L)).thenReturn(true);
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "PROYECTADO")).thenReturn(BigDecimal.ZERO);
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "DESEMBOLSADO")).thenReturn(new BigDecimal("14.00"));

        ResumenGastoEventoDTO resumen = gastoService.getResumen(1L);

        assertEquals(BigDecimal.ZERO, resumen.totalProyectado());
        assertEquals(new BigDecimal("14.00"), resumen.totalDesembolsado());
        assertEquals(BigDecimal.ZERO, resumen.totalPendiente());
    }

    @Test
    void mezcla_totalesCorrectos() {
        when(eventoRepository.existsById(1L)).thenReturn(true);
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "PROYECTADO")).thenReturn(new BigDecimal("50.00"));
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "DESEMBOLSADO")).thenReturn(new BigDecimal("30.00"));

        ResumenGastoEventoDTO resumen = gastoService.getResumen(1L);

        assertEquals(new BigDecimal("50.00"), resumen.totalProyectado());
        assertEquals(new BigDecimal("30.00"), resumen.totalDesembolsado());
        assertEquals(new BigDecimal("50.00"), resumen.totalPendiente());
    }

    @Test
    void eventoInexistente_lanza404() {
        when(eventoRepository.existsById(99L)).thenReturn(false);

        assertThrows(EventoNotFoundException.class,
                () -> gastoService.getResumen(99L));
    }

    @Test
    void totalPendiente_igualATotalProyectado() {
        when(eventoRepository.existsById(1L)).thenReturn(true);
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "PROYECTADO")).thenReturn(new BigDecimal("100.00"));
        when(gastoRepository.sumMontoByEventoIdAndEstado(1L, "DESEMBOLSADO")).thenReturn(new BigDecimal("40.00"));

        ResumenGastoEventoDTO resumen = gastoService.getResumen(1L);

        assertEquals(resumen.totalProyectado(), resumen.totalPendiente());
    }

    @Test
    void obtenerEventosControl_retornaCola() {
        Object[] row1 = new Object[]{
            1L, "Cliente A", "EN_MONTAJE",
            java.sql.Date.valueOf(LocalDate.of(2026, 6, 1)),
            java.sql.Time.valueOf(LocalTime.of(10, 0)),
            java.sql.Time.valueOf(LocalTime.of(18, 0)),
            "Salon Central",
            BigDecimal.valueOf(500), BigDecimal.valueOf(200),
            3L, 2L, 1L
        };
        when(gastoRepository.findEventosControlGastosNative())
            .thenReturn(Collections.singletonList(row1));

        List<EventoGastoControlDTO> resultado = gastoService.obtenerEventosControl();

        assertEquals(1, resultado.size());
        EventoGastoControlDTO dto = resultado.get(0);
        assertEquals(1L, dto.getEventoId());
        assertEquals("Cliente A", dto.getClienteNombre());
        assertEquals("EN_MONTAJE", dto.getEstadoEventoNombre());
        assertEquals(BigDecimal.valueOf(500), dto.getTotalProyectado());
        assertEquals(BigDecimal.valueOf(500), dto.getTotalPendiente());
        assertEquals(BigDecimal.valueOf(200), dto.getTotalDesembolsado());
        assertEquals(3L, dto.getCantidadGastos().longValue());
        assertEquals(2L, dto.getCantidadProyectados().longValue());
        assertEquals(1L, dto.getCantidadDesembolsados().longValue());
        assertTrue(dto.getPermiteRegistrarGasto());
    }

    @Test
    void obtenerEventosControl_sinGastos_devuelveCeros() {
        Object[] row1 = new Object[]{
            1L, "Cliente B", "PROGRAMADO",
            java.sql.Date.valueOf(LocalDate.of(2026, 7, 1)),
            null, null, "Salon B",
            BigDecimal.ZERO, BigDecimal.ZERO,
            0L, 0L, 0L
        };
        when(gastoRepository.findEventosControlGastosNative())
            .thenReturn(Collections.singletonList(row1));

        List<EventoGastoControlDTO> resultado = gastoService.obtenerEventosControl();

        assertEquals(1, resultado.size());
        assertEquals(0, resultado.get(0).getCantidadGastos());
        assertEquals(BigDecimal.ZERO, resultado.get(0).getTotalProyectado());
        assertTrue(resultado.get(0).getPermiteRegistrarGasto());
    }

    @Test
    void obtenerEventosControl_estadoNoPermitido_marcaFalse() {
        Object[] row1 = new Object[]{
            1L, "Cliente C", "FINALIZADO",
            java.sql.Date.valueOf(LocalDate.of(2026, 5, 1)),
            java.sql.Time.valueOf(LocalTime.of(10, 0)),
            java.sql.Time.valueOf(LocalTime.of(18, 0)),
            null,
            BigDecimal.ZERO, BigDecimal.ZERO,
            0L, 0L, 0L
        };
        when(gastoRepository.findEventosControlGastosNative())
            .thenReturn(Collections.singletonList(row1));

        List<EventoGastoControlDTO> resultado = gastoService.obtenerEventosControl();

        assertFalse(resultado.get(0).getPermiteRegistrarGasto());
    }
}
