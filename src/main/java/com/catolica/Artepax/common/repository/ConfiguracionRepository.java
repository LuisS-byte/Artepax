package com.catolica.Artepax.common.repository;

import com.catolica.Artepax.common.entity.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio para entidades de configuración global.
 */
public interface ConfiguracionRepository extends JpaRepository<Configuracion, String> {
}
