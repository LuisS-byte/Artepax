package com.catolica.Artepax.pago.repository;

import com.catolica.Artepax.pago.entity.ConceptoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConceptoPagoRepository extends JpaRepository<ConceptoPago, Long> {
    Optional<ConceptoPago> findByConceptoNombre(String conceptoNombre);
}
