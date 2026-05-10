package com.catolica.Artepax.common.service;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import com.catolica.Artepax.common.audit.ProductoAuditSnapshot;
import com.catolica.Artepax.common.entity.Auditoria;
import com.catolica.Artepax.common.repository.AuditoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private AuditoriaService auditoriaService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditoriaService = new AuditoriaService(auditoriaRepository, usuarioRepository, objectMapper);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("admin@test.com");
        lenient().when(usuarioRepository.findByCorreo("admin@test.com"))
                .thenReturn(Optional.of(new Usuario()));
        lenient().when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registrar_conValoresString_guardaCorrectamente() {
        SecurityContextHolder.setContext(securityContext);

        auditoriaService.registrar("CREAR", "evento", 1L, null, "{\"id\":1}", "Evento creado");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria saved = captor.getValue();

        assertEquals("CREAR", saved.getAccion());
        assertEquals("evento", saved.getTablaAfectada());
        assertEquals(1L, saved.getIdRegistro());
        assertNull(saved.getValorAnterior());
        assertEquals("{\"id\":1}", saved.getValorNuevo());
        assertEquals("Evento creado", saved.getDescripcion());
    }

    @Test
    void registrar_conObjetos_serializaAJSON() {
        SecurityContextHolder.setContext(securityContext);

        ProductoAuditSnapshot snapshot = new ProductoAuditSnapshot(1L, "Mantel", 10, "ACTIVO");
        auditoriaService.registrar("AJUSTAR_STOCK", "producto", 1L, null, snapshot, "Ajuste manual");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria saved = captor.getValue();

        assertNotNull(saved.getValorNuevo());
        assertTrue(saved.getValorNuevo().contains("Mantel"));
        assertTrue(saved.getValorNuevo().contains("10"));
        assertTrue(saved.getValorNuevo().contains("ACTIVO"));
    }

    @Test
    void registrar_conValorAnteriorNull_permite() {
        SecurityContextHolder.setContext(securityContext);

        auditoriaService.registrar("CREAR", "pago", 5L, null, "{\"monto\":100}", "Pago creado");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertNull(captor.getValue().getValorAnterior());
    }

    @Test
    void registrar_conValorNuevoNull_permite() {
        SecurityContextHolder.setContext(securityContext);

        auditoriaService.registrar("EDITAR", "evento", 3L, "{\"estado\":\"ACTIVO\"}", null, "Estado cambiado");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertNull(captor.getValue().getValorNuevo());
    }

    @Test
    void registrar_sinUsuarioAutenticado_noFalla() {
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("desconocido@test.com");
        when(usuarioRepository.findByCorreo("desconocido@test.com")).thenReturn(Optional.empty());

        auditoriaService.registrar("CREAR", "evento", 1L, null, "{}", "Sin usuario");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertNull(captor.getValue().getUsuario());
    }

    @Test
    void registrar_conExcepcionInterna_noLanzaError() {
        SecurityContextHolder.setContext(securityContext);
        when(auditoriaRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() ->
                auditoriaService.registrar("CREAR", "evento", 1L, null, "{}", "Falla silenciosa"));
    }

    @Test
    void registrar_serializaSnapshotCompleto() {
        SecurityContextHolder.setContext(securityContext);

        ProductoAuditSnapshot antes = new ProductoAuditSnapshot(1L, "Mantel", 10, "ACTIVO");
        ProductoAuditSnapshot despues = new ProductoAuditSnapshot(1L, "Mantel", 15, "ACTIVO");
        auditoriaService.registrar("AJUSTAR_STOCK", "producto", 1L, antes, despues, "Ajuste +5");

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria saved = captor.getValue();

        assertNotNull(saved.getValorAnterior());
        assertNotNull(saved.getValorNuevo());
        assertTrue(saved.getValorAnterior().contains("10"));
        assertTrue(saved.getValorNuevo().contains("15"));
    }
}
