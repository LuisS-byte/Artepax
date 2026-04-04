package com.catolica.Artepax.evento.repository;

import com.catolica.Artepax.evento.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

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
}