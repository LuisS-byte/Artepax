package com.catolica.Artepax.pago.repository;


import com.catolica.Artepax.pago.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByEventoId(Long eventoId);
    Optional<Pago> findFirstByEventoId(Long eventoId);
}