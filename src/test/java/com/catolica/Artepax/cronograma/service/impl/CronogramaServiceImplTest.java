package com.catolica.Artepax.cronograma.service.impl;

import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.cronograma.dto.CronogramaResponseDTO;
import com.catolica.Artepax.cronograma.dto.CronogramaUpdateDTO;
import com.catolica.Artepax.cronograma.entity.CronogramaAcciones;
import com.catolica.Artepax.cronograma.entity.Cronograma;
import com.catolica.Artepax.cronograma.entity.CronogramaEstado;
import com.catolica.Artepax.cronograma.entity.CronogramaEstados;
import com.catolica.Artepax.cronograma.exception.CronogramaException;
import com.catolica.Artepax.cronograma.mapper.CronogramaMapper;
import com.catolica.Artepax.cronograma.repository.CronogramaRepository;
import com.catolica.Artepax.cronograma.repository.CronogramaUsuarioRepository;
import com.catolica.Artepax.cronograma.service.policy.CronogramaPolicy;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.EstadoEventoNames;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CronogramaServiceImplTest {

    @Mock
    private CronogramaRepository cronogramaRepository;
    @Mock
    private CronogramaUsuarioRepository cronogramaUsuarioRepository;
    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private CronogramaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CronogramaServiceImpl(
                cronogramaRepository,
                cronogramaUsuarioRepository,
                new CronogramaMapper(new CronogramaPolicy()),
                eventoRepository,
                usuarioRepository,
                new CronogramaPolicy());
    }

    @Test
    void pendienteAEnProgreso_permitido() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.PENDIENTE.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));
        when(cronogramaRepository.save(any(Cronograma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CronogramaResponseDTO response = service.cambiarEstado(1L, CronogramaEstado.EN_PROGRESO.getValor());

        assertEquals(CronogramaEstado.EN_PROGRESO.getValor(), response.getCronoEstado());
        assertEquals(true, response.getEditable());
        assertEquals(true, response.getCancelable());
        assertEquals(java.util.List.of(CronogramaAcciones.COMPLETAR, CronogramaAcciones.CANCELAR), response.getAccionesPermitidas());
        assertEquals(java.util.List.of(CronogramaEstados.COMPLETADO, CronogramaEstados.CANCELADO), response.getEstadosSiguientesPermitidos());
    }

    @Test
    void enProgresoACompletado_permitido() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.EN_PROGRESO.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));
        when(cronogramaRepository.save(any(Cronograma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CronogramaResponseDTO response = service.cambiarEstado(1L, CronogramaEstado.COMPLETADO.getValor());

        assertEquals(CronogramaEstado.COMPLETADO.getValor(), response.getCronoEstado());
        assertEquals(false, response.getEditable());
        assertEquals(false, response.getCancelable());
        assertEquals(java.util.List.of(), response.getAccionesPermitidas());
    }

    @Test
    void pendienteACancelado_permitido() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.PENDIENTE.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));
        when(cronogramaRepository.save(any(Cronograma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelar(1L);

        assertEquals(CronogramaEstado.CANCELADO.getValor(), cronograma.getCronoEstado());
        verify(cronogramaRepository).save(cronograma);
    }

    @Test
    void enProgresoACancelado_permitido() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.EN_PROGRESO.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));
        when(cronogramaRepository.save(any(Cronograma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelar(1L);

        assertEquals(CronogramaEstado.CANCELADO.getValor(), cronograma.getCronoEstado());
        verify(cronogramaRepository).save(cronograma);
    }

    @Test
    void pendienteACompletado_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.PENDIENTE.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class,
                () -> service.cambiarEstado(1L, CronogramaEstado.COMPLETADO.getValor()));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    @Test
    void completadoAPendiente_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.COMPLETADO.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class,
                () -> service.cambiarEstado(1L, CronogramaEstado.PENDIENTE.getValor()));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    @Test
    void canceladoAEnProgreso_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.CANCELADO.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class,
                () -> service.cambiarEstado(1L, CronogramaEstado.EN_PROGRESO.getValor()));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    @Test
    void actualizarActividadCompletada_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.COMPLETADO.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class, () -> service.actualizar(1L, crearUpdateValido()));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    @Test
    void cancelarActividadCompletada_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.COMPLETADO.getValor(), EstadoEventoNames.CONFIRMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class, () -> service.cancelar(1L));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    @Test
    void actualizarActividadFueraDelDiaDelEvento_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.PENDIENTE.getValor(), EstadoEventoNames.CONFIRMADO);
        CronogramaUpdateDTO dto = new CronogramaUpdateDTO();
        dto.setFechaInicio(LocalDateTime.of(2026, 5, 11, 10, 0));
        dto.setFechaFin(LocalDateTime.of(2026, 5, 11, 11, 0));
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class, () -> service.actualizar(1L, dto));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    @Test
    void actualizarActividadConFechaFinAntesDeInicio_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.PENDIENTE.getValor(), EstadoEventoNames.CONFIRMADO);
        CronogramaUpdateDTO dto = new CronogramaUpdateDTO();
        dto.setFechaInicio(LocalDateTime.of(2026, 5, 10, 12, 0));
        dto.setFechaFin(LocalDateTime.of(2026, 5, 10, 11, 0));
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class, () -> service.actualizar(1L, dto));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    @Test
    void actualizarActividadDentroDelRango_permitido() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.PENDIENTE.getValor(), EstadoEventoNames.EN_MONTAJE);
        CronogramaUpdateDTO dto = crearUpdateValido();
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));
        when(cronogramaRepository.save(any(Cronograma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CronogramaResponseDTO response = service.actualizar(1L, dto);

        assertEquals("Actividad actualizada", response.getActividad());
        assertEquals(LocalDateTime.of(2026, 5, 10, 11, 0), response.getFechaInicio());
        assertEquals(LocalDateTime.of(2026, 5, 10, 12, 0), response.getFechaFin());
        verify(cronogramaRepository).save(cronograma);
    }

    @Test
    void actualizarActividadFueraDelHorarioDelEvento_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.PENDIENTE.getValor(), EstadoEventoNames.CONFIRMADO);
        CronogramaUpdateDTO dto = new CronogramaUpdateDTO();
        dto.setFechaInicio(LocalDateTime.of(2026, 5, 10, 9, 0));
        dto.setFechaFin(LocalDateTime.of(2026, 5, 10, 11, 0));
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class, () -> service.actualizar(1L, dto));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    @Test
    void actualizarActividadEnEventoNoPermitido_lanzaError() {
        Cronograma cronograma = crearCronograma(1L, CronogramaEstado.PENDIENTE.getValor(), EstadoEventoNames.PROGRAMADO);
        when(cronogramaRepository.findById(1L)).thenReturn(Optional.of(cronograma));

        assertThrows(CronogramaException.class, () -> service.actualizar(1L, crearUpdateValido()));

        verify(cronogramaRepository, never()).save(any(Cronograma.class));
    }

    private CronogramaUpdateDTO crearUpdateValido() {
        CronogramaUpdateDTO dto = new CronogramaUpdateDTO();
        dto.setActividad("Actividad actualizada");
        dto.setFechaInicio(LocalDateTime.of(2026, 5, 10, 11, 0));
        dto.setFechaFin(LocalDateTime.of(2026, 5, 10, 12, 0));
        dto.setCronoObservacion("Obs");
        return dto;
    }

    private Cronograma crearCronograma(Long id, String estadoCronograma, String estadoEventoNombre) {
        Evento evento = new Evento();
        setEventoId(evento, 10L);
        evento.setCliente(new Cliente());
        evento.setDecoracion(new Decoracion());
        evento.setTipoEvento(new TipoEvento());
        evento.setEstadoEvento(crearEstadoEvento(estadoEventoNombre));
        evento.setFecha(LocalDate.of(2026, 5, 10));
        evento.setHoraInicio(LocalTime.of(10, 0));
        evento.setHoraFin(LocalTime.of(14, 0));
        evento.setPrecioBase(new BigDecimal("100.00"));
        evento.setPrecioAdicional(BigDecimal.ZERO);
        evento.setClienteNotificado(false);

        Cronograma cronograma = new Cronograma();
        setCronogramaId(cronograma, id);
        cronograma.setEvento(evento);
        cronograma.setActividad("Actividad base");
        cronograma.setFechaInicio(LocalDateTime.of(2026, 5, 10, 10, 30));
        cronograma.setFechaFin(LocalDateTime.of(2026, 5, 10, 11, 30));
        cronograma.setCronoEstado(estadoCronograma);
        return cronograma;
    }

    private EstadoEvento crearEstadoEvento(String nombre) {
        EstadoEvento estadoEvento = new EstadoEvento();
        estadoEvento.setNombre(nombre);
        estadoEvento.setDescripcion(nombre);
        return estadoEvento;
    }

    private void setCronogramaId(Cronograma cronograma, Long id) {
        try {
            var field = Cronograma.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(cronograma, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setEventoId(Evento evento, Long id) {
        try {
            var field = Evento.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(evento, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
