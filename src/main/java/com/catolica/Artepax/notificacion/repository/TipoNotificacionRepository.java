package com.catolica.Artepax.notificacion.repository;

import com.catolica.Artepax.notificacion.entity.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoNotificacionRepository extends JpaRepository<TipoNotificacion, Long> {
    Optional<TipoNotificacion> findByNombreTipo(String nombreTipo);
}
