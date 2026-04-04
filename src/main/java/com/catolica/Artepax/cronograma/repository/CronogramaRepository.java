package com.catolica.Artepax.cronograma.repository;

import com.catolica.Artepax.cronograma.entity.Cronograma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CronogramaRepository extends JpaRepository<Cronograma, Long> {

    List<Cronograma> findByIdEvento(Long idEvento);
}
