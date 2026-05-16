package com.catolica.Artepax.authentication.user.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UsuarioEstadoRequest}.
 * <p>
 * Verifies record construction and field accessor.
 */
class UsuarioEstadoRequestTest {

    @Test
    void constructor_shouldSetActivoTrue() {
        var request = new UsuarioEstadoRequest(true);
        assertThat(request.activo()).isTrue();
    }

    @Test
    void constructor_shouldSetActivoFalse() {
        var request = new UsuarioEstadoRequest(false);
        assertThat(request.activo()).isFalse();
    }
}
