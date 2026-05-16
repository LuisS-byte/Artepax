package com.catolica.Artepax.authentication.user.repository;

import com.catolica.Artepax.authentication.user.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Rol} entities.
 * <p>
 * The primary key type is {@link Integer}, matching {@code Rol.idRol}.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
}
