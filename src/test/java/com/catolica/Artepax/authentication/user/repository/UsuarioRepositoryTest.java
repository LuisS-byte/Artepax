package com.catolica.Artepax.authentication.user.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies new method signatures added to {@link UsuarioRepository}.
 * <p>
 * Structural verification — confirms the added methods compile.
 * Actual query behavior is verified in Phase 4 integration tests.
 */
class UsuarioRepositoryTest {

    @Test
    void method_existsByCorreoIgnoreCase_shouldExist() {
        var method = findMethod("existsByCorreoIgnoreCase");
        assertThat(method).isPresent();
        assertThat(method.get().getParameterCount()).isEqualTo(1);
        assertThat(method.get().getParameterTypes()[0]).isEqualTo(String.class);
        assertThat(method.get().getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    void method_countActivosByRolNombre_shouldExist() {
        var method = findMethod("countActivosByRolNombre");
        assertThat(method).isPresent();
        assertThat(method.get().getParameterCount()).isEqualTo(1);
        assertThat(method.get().getParameterTypes()[0]).isEqualTo(String.class);
        assertThat(method.get().getReturnType()).isEqualTo(long.class);
    }

    private static java.util.Optional<java.lang.reflect.Method> findMethod(String name) {
        for (var m : UsuarioRepository.class.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return java.util.Optional.of(m);
            }
        }
        return java.util.Optional.empty();
    }
}
