package com.catolica.Artepax.common.repository;

import com.catolica.Artepax.common.entity.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio para entidades de configuración global.
 * La clave primaria es {@code configClave} (String).
 */
public interface ConfiguracionRepository extends JpaRepository<Configuracion, String> {

    /**
     * Retorna todas las configuraciones ordenadas alfabéticamente por clave.
     *
     * @return lista de configuraciones ordenadas por clave ascendente
     */
    List<Configuracion> findAllByOrderByConfigClaveAsc();
}
