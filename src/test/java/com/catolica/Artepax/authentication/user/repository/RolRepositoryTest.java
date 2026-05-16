package com.catolica.Artepax.authentication.user.repository;

import com.catolica.Artepax.authentication.user.entity.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link RolRepository} contract.
 * <p>
 * Structural verification — confirms the repository extends {@code JpaRepository<Rol, Integer>}
 * with the correct generic type matching {@code Rol.idRol} (Integer).
 */
class RolRepositoryTest {

    @Test
    void interface_shouldExtendJpaRepositoryWithIntegerId() {
        assertThat(RolRepository.class).isInterface();
        assertThat(JpaRepository.class.isAssignableFrom(RolRepository.class)).isTrue();
    }

    @Test
    void idType_shouldMatchRolIdRol() {
        // Rol.idRol is Integer — verify the repository's ID generic matches
        var genericInterfaces = RolRepository.class.getGenericInterfaces();
        assertThat(genericInterfaces).hasSizeGreaterThan(0);

        // JpaRepository<Rol, Integer> means the second type arg should be Integer
        boolean matchesInteger = false;
        for (var gi : genericInterfaces) {
            if (gi instanceof java.lang.reflect.ParameterizedType pt
                    && pt.getRawType() == JpaRepository.class) {
                var typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length == 2 && typeArgs[1] == Integer.class) {
                    matchesInteger = true;
                }
            }
        }
        assertThat(matchesInteger)
                .as("RolRepository should extend JpaRepository<Rol, Integer>")
                .isTrue();
    }
}
