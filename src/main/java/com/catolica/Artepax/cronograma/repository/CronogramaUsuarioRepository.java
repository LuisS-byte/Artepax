package com.catolica.Artepax.cronograma.repository;

import com.catolica.Artepax.cronograma.entity.CronogramaUsuario;
import com.catolica.Artepax.cronograma.entity.CronogramaUsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CronogramaUsuarioRepository extends JpaRepository<CronogramaUsuario, CronogramaUsuarioId> {
    List<CronogramaUsuario> findByIdIdCronograma(Long idCronograma);

    // Para ver todas las actividades asignadas a un usuario
    List<CronogramaUsuario> findByIdIdUsuario(Long idUsuario);
}
