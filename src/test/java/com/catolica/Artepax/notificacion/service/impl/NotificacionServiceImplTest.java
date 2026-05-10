package com.catolica.Artepax.notificacion.service.impl;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import com.catolica.Artepax.notificacion.entity.EstadoNotificacion;
import com.catolica.Artepax.notificacion.entity.Notificacion;
import com.catolica.Artepax.notificacion.entity.TipoNotificacion;
import com.catolica.Artepax.notificacion.exception.NotificacionNotFoundException;
import com.catolica.Artepax.notificacion.repository.EstadoNotificacionRepository;
import com.catolica.Artepax.notificacion.repository.NotificacionRepository;
import com.catolica.Artepax.notificacion.repository.TipoNotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private TipoNotificacionRepository tipoRepository;
    @Mock
    private EstadoNotificacionRepository estadoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private NotificacionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificacionServiceImpl(
                notificacionRepository,
                tipoRepository,
                estadoRepository,
                usuarioRepository);
    }

    @Test
    void duenioMarcaLeida_permitido() {
        Notificacion notificacion = crearNotificacion(10L, 5L, "PENDIENTE");
        when(notificacionRepository.findById(10L)).thenReturn(Optional.of(notificacion));
        when(estadoRepository.findByNombreEstado("LEIDA")).thenReturn(Optional.of(crearEstado("LEIDA")));

        service.marcarLeida(10L, 5L);

        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void otroUsuarioMarcaLeida_lanzaAccessDenied() {
        Notificacion notificacion = crearNotificacion(10L, 5L, "PENDIENTE");
        when(notificacionRepository.findById(10L)).thenReturn(Optional.of(notificacion));

        assertThrows(AccessDeniedException.class, () -> service.marcarLeida(10L, 9L));

        verify(estadoRepository, never()).findByNombreEstado("LEIDA");
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void notificacionInexistente_lanzaNotFound() {
        when(notificacionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotificacionNotFoundException.class, () -> service.marcarLeida(10L, 5L));

        verify(estadoRepository, never()).findByNombreEstado("LEIDA");
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void notificacionYaLeida_esIdempotente() {
        Notificacion notificacion = crearNotificacion(10L, 5L, "LEIDA");
        when(notificacionRepository.findById(10L)).thenReturn(Optional.of(notificacion));

        service.marcarLeida(10L, 5L);

        verify(estadoRepository, never()).findByNombreEstado("LEIDA");
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    private Notificacion crearNotificacion(Long notificacionId, Long usuarioId, String estadoNombre) {
        Notificacion notificacion = new Notificacion();
        Usuario usuario = new Usuario();
        setField(usuario, "idUsuario", usuarioId);
        EstadoNotificacion estado = crearEstado(estadoNombre);
        TipoNotificacion tipo = new TipoNotificacion();
        tipo.setNombreTipo("TEST");

        setField(notificacion, "id", notificacionId);
        notificacion.setUsuario(usuario);
        notificacion.setEstadoNotificacion(estado);
        notificacion.setTipoNotificacion(tipo);
        notificacion.setMensaje("Mensaje");
        return notificacion;
    }

    private EstadoNotificacion crearEstado(String nombre) {
        EstadoNotificacion estado = new EstadoNotificacion();
        estado.setNombreEstado(nombre);
        estado.setDescripcion(nombre);
        return estado;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
