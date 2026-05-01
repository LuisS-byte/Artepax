package com.catolica.Artepax.notificacion.repository;

import com.catolica.Artepax.notificacion.entity.EstadoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoNotificacionRepository extends JpaRepository<EstadoNotificacion, Long> {
    Optional<EstadoNotificacion> findByNombreEstado(String nombreEstado);
}
