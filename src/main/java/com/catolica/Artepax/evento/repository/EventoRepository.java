package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    boolean existsByFechaAndHoraInicioAndUbicacion(
        LocalDate fecha,
        LocalTime horaInicio,
        String ubicacion
    );

    List<Evento> findByClienteId(Long clienteId);

    // para disponibilidad
    List<Evento> findByFecha(LocalDate fecha);

    // para calendario por rango de fechas
    List<Evento> findByFechaBetween(LocalDate inicio, LocalDate fin);

    @Query("SELECT e FROM Evento e JOIN e.estadoEvento ee " +
            "WHERE ee.nombre = 'CANCELACION_PENDIENTE'")
    List<Evento> findEnCancelacionPendiente();

    // Para job de cotizaciones vencidas
    @Query("SELECT e FROM Evento e JOIN e.estadoEvento ee " +
            "WHERE ee.nombre = 'COTIZACION' " +
            "AND e.fechaExpiracion IS NOT NULL " +
            "AND e.fechaExpiracion < :hoy")
    List<Evento> findCotizacionesVencidas(@Param("hoy") LocalDate hoy);
}