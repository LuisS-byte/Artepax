package com.catolica.Artepax.cronograma.repository;

import com.catolica.Artepax.cronograma.entity.Cronograma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CronogramaRepository extends JpaRepository<Cronograma, Long> {

    List<Cronograma> findByEventoId(Long eventoId);


    // Cancelación en cascada cuando se cancela el evento
    @Modifying
    @Query("UPDATE Cronograma c SET c.cronoEstado = 'CANCELADO' " +
            "WHERE c.evento.id = :eventoId " +
            "AND c.cronoEstado NOT IN ('COMPLETADO', 'CANCELADO')")
    void cancelarPorEvento(@Param("eventoId") Long eventoId);
}
