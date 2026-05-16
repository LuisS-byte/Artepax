package com.catolica.Artepax.authentication.user.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UsuarioCreateRequest}.
 * <p>
 * Verifies record construction and field accessors.
 */
class UsuarioCreateRequestTest {

    @Test
    void constructor_shouldSetAllFields() {
        var request = new UsuarioCreateRequest(
                "Juan Pérez",
                "juan@example.com",
                "securePass123",
                "555-0100",
                1
        );

        assertThat(request.nombre()).isEqualTo("Juan Pérez");
        assertThat(request.correo()).isEqualTo("juan@example.com");
        assertThat(request.password()).isEqualTo("securePass123");
        assertThat(request.telefono()).isEqualTo("555-0100");
        assertThat(request.rolId()).isEqualTo(1);
    }

    @Test
    void constructor_shouldAllowNullTelefono() {
        var request = new UsuarioCreateRequest(
                "Ana Gómez",
                "ana@example.com",
                "password123",
                null,
                2
        );

        assertThat(request.nombre()).isEqualTo("Ana Gómez");
        assertThat(request.correo()).isEqualTo("ana@example.com");
        assertThat(request.password()).isEqualTo("password123");
        assertThat(request.telefono()).isNull();
        assertThat(request.rolId()).isEqualTo(2);
    }
}
