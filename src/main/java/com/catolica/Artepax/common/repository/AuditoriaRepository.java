package com.catolica.Artepax.common.repository;

import com.catolica.Artepax.common.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findByTablaAfectadaOrderByFechaDesc(String tablaAfectada);
    List<Auditoria> findByIdRegistroAndTablaAfectadaOrderByFechaDesc(Long idRegistro, String tablaAfectada);
}
