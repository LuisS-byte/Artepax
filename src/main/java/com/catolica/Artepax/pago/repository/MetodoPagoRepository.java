package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {
   Optional<MetodoPago> findByMetodoNombre(String nombre);
   Optional<MetodoPago> findByMetodoNombreIgnoreCase(String nombre);
}
