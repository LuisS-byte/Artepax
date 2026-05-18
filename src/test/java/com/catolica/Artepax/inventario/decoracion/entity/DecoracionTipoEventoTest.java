package com.catolica.Artepax.inventario.decoracion.entity;

import com.catolica.Artepax.evento.model.TipoEvento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link DecoracionTipoEvento} junction entity and its
 * {@link DecoracionTipoEventoId} embeddable key.
 * <p>
 * Verifies constructor behavior, getters, equals/hashCode contract,
 * and the {@code @MapsId} pattern used for the composite key.
 * </p>
 */
class DecoracionTipoEventoTest {

    @Test
    void decoracionTipoEventoId_constructsAndGettersWork() {
        DecoracionTipoEventoId id = new DecoracionTipoEventoId(1L, 2L);

        assertEquals(1L, id.getDecoracionId());
        assertEquals(2L, id.getTipoEventoId());
    }

    @Test
    void decoracionTipoEventoId_equalsAndHashCode() {
        DecoracionTipoEventoId id1 = new DecoracionTipoEventoId(1L, 2L);
        DecoracionTipoEventoId id2 = new DecoracionTipoEventoId(1L, 2L);
        DecoracionTipoEventoId id3 = new DecoracionTipoEventoId(3L, 4L);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
        assertNotEquals(id1, null);
        assertNotEquals(id1, "some string");
    }

    @Test
    void decoracionTipoEventoId_equalsSameReference() {
        DecoracionTipoEventoId id = new DecoracionTipoEventoId(1L, 1L);
        assertTrue(id.equals(id));
    }

    @Test
    void decoracionTipoEvento_constructsAndGettersWork() {
        Decoracion decoracion = new Decoracion();
        // Use reflection to set the ID on Decoracion (no public setter for id)
        setId(decoracion, 10L);

        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setId(20L);
        tipoEvento.setTipoNombre("BODA");
        tipoEvento.setTipoActivo(true);

        DecoracionTipoEvento dte = new DecoracionTipoEvento(decoracion, tipoEvento);

        assertNotNull(dte.getId());
        assertEquals(10L, dte.getId().getDecoracionId());
        assertEquals(20L, dte.getId().getTipoEventoId());
        assertSame(decoracion, dte.getDecoracion());
        assertSame(tipoEvento, dte.getTipoEvento());
        assertNotNull(dte.getFechaCreacion());
        assertTrue(dte.getFechaCreacion().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void decoracionTipoEvento_mapsIdPatternIsCorrect() {
        Decoracion decoracion = new Decoracion();
        setId(decoracion, 99L);

        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setId(88L);

        DecoracionTipoEvento dte = new DecoracionTipoEvento(decoracion, tipoEvento);

        // The @MapsId pattern should copy the decoracion's ID into the embedded key
        assertEquals(99L, dte.getId().getDecoracionId());
        assertEquals(88L, dte.getId().getTipoEventoId());
    }

    /**
     * Sets the private {@code id} field on Decoracion using reflection,
     * matching the pattern used in {@code EventoServiceImplTest.createProducto}.
     */
    private void setId(Decoracion decoracion, Long id) {
        try {
            var idField = Decoracion.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(decoracion, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
