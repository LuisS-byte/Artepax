package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.VistaEventosPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
public interface VistaEventosPeriodoRepository extends JpaRepository<VistaEventosPeriodo, Long> {
    List<VistaEventosPeriodo> findByFechaEventoBetween(LocalDate inicio, LocalDate fin);
}