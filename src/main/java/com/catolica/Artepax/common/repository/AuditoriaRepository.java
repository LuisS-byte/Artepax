package com.catolica.Artepax.common.repository;

import com.catolica.Artepax.common.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio para entidades de auditoría.
 * Provee consultas para filtrar registros por tabla afectada y por registro específico.
 */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /**
     * Busca todos los registros de auditoría para una tabla, ordenados por fecha descendente.
     *
     * @param tablaAfectada el nombre de la tabla auditada
     * @return lista de registros de auditoría
     */
    List<Auditoria> findByTablaAfectadaOrderByFechaDesc(String tablaAfectada);

    /**
     * Busca todos los registros de auditoría para un registro específico en una tabla,
     * ordenados por fecha descendente.
     *
     * @param idRegistro     el ID del registro auditado
     * @param tablaAfectada el nombre de la tabla auditada
     * @return lista de registros de auditoría
     */
    List<Auditoria> findByIdRegistroAndTablaAfectadaOrderByFechaDesc(Long idRegistro, String tablaAfectada);
}
