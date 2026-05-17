package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.VistaEventosPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para la vista de base de datos {@code vista_eventos_periodo}.
 * <p>
 * Proporciona consultas para el calendario de eventos por rango de fechas.
 */
public interface VistaEventosPeriodoRepository extends JpaRepository<VistaEventosPeriodo, Long> {

    /**
     * Retorna los eventos dentro de un rango de fechas.
     *
     * @param inicio fecha de inicio del rango
     * @param fin    fecha de fin del rango
     * @return lista de eventos en el período
     */
    List<VistaEventosPeriodo> findByFechaEventoBetween(LocalDate inicio, LocalDate fin);

    /**
     * Retorna los eventos dentro de un rango de fechas ordenados por fecha y hora.
     *
     * @param inicio fecha de inicio del rango
     * @param fin    fecha de fin del rango
     * @return lista ordenada de eventos en el período
     */
    List<VistaEventosPeriodo> findByFechaEventoBetweenOrderByFechaEventoAscHoraInicioAsc(LocalDate inicio, LocalDate fin);
}
