package com.catolica.Artepax.cronograma.repository;

import com.catolica.Artepax.cronograma.entity.CronogramaUsuario;
import com.catolica.Artepax.cronograma.entity.CronogramaUsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio para asignaciones de usuarios a actividades del cronograma.
 */
public interface CronogramaUsuarioRepository extends JpaRepository<CronogramaUsuario, CronogramaUsuarioId> {

    List<CronogramaUsuario> findByIdIdCronograma(Long idCronograma);

    /**
     * Obtiene todas las actividades asignadas a un usuario.
     */
    List<CronogramaUsuario> findByIdIdUsuario(Long idUsuario);
}
