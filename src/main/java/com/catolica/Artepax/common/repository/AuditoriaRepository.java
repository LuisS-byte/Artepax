package com.catolica.Artepax.common.repository;

import com.catolica.Artepax.common.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio para entidades de auditoría.
 */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findByTablaAfectadaOrderByFechaDesc(String tablaAfectada);
    List<Auditoria> findByIdRegistroAndTablaAfectadaOrderByFechaDesc(Long idRegistro, String tablaAfectada);
}
