package com.catolica.Artepax.cronograma.repository;

import com.catolica.Artepax.cronograma.entity.CronogramaUsuario;
import com.catolica.Artepax.cronograma.entity.CronogramaUsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio para asignaciones de usuarios a actividades del cronograma.
 */
public interface CronogramaUsuarioRepository extends JpaRepository<CronogramaUsuario, CronogramaUsuarioId> {

    List<CronogramaUsuario> findByIdIdCronograma(Long idCronograma);

    @Query("""
            SELECT cu
            FROM CronogramaUsuario cu
            JOIN FETCH cu.usuario
            WHERE cu.id.idCronograma = :cronogramaId
            ORDER BY cu.usuario.nombre ASC
            """)
    List<CronogramaUsuario> findByCronogramaIdWithUsuario(@Param("cronogramaId") Long cronogramaId);

    /**
     * Obtiene todas las actividades asignadas a un usuario.
     */
    List<CronogramaUsuario> findByIdIdUsuario(Long idUsuario);
}
