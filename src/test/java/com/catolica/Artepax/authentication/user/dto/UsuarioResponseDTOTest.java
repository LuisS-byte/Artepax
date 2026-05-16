package com.catolica.Artepax.authentication.user.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UsuarioResponseDTO}.
 * <p>
 * Verifies record construction and field accessors.
 */
class UsuarioResponseDTOTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 5, 16, 15, 0);

    @Test
    void constructor_shouldSetAllFields() {
        var dto = new UsuarioResponseDTO(
                1L,
                "Juan Pérez",
                "juan@example.com",
                "555-0100",
                "LOCAL",
                true,
                1,
                "ADMIN",
                now
        );

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nombre()).isEqualTo("Juan Pérez");
        assertThat(dto.correo()).isEqualTo("juan@example.com");
        assertThat(dto.telefono()).isEqualTo("555-0100");
        assertThat(dto.tipoAuth()).isEqualTo("LOCAL");
        assertThat(dto.estado()).isTrue();
        assertThat(dto.rolId()).isEqualTo(1);
        assertThat(dto.rol()).isEqualTo("ADMIN");
        assertThat(dto.fechaCreacion()).isEqualTo(now);
    }

    @Test
    void constructor_shouldAllowNullTelefono() {
        var dto = new UsuarioResponseDTO(
                2L,
                "Ana Gómez",
                "ana@example.com",
                null,
                "GOOGLE",
                false,
                2,
                "EMPLOYEE",
                now
        );

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.nombre()).isEqualTo("Ana Gómez");
        assertThat(dto.correo()).isEqualTo("ana@example.com");
        assertThat(dto.telefono()).isNull();
        assertThat(dto.tipoAuth()).isEqualTo("GOOGLE");
        assertThat(dto.estado()).isFalse();
        assertThat(dto.rolId()).isEqualTo(2);
        assertThat(dto.rol()).isEqualTo("EMPLOYEE");
        assertThat(dto.fechaCreacion()).isEqualTo(now);
    }
}
