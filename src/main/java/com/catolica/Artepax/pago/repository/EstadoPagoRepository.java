package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoPagoRepository extends JpaRepository<EstadoPago, Long> {


    Optional<EstadoPago> findByEstadoNombre(String nombre);
}
