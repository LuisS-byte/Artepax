package com.catolica.Artepax.clientes.repository;

import com.catolica.Artepax.clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Cliente} entity operations.
 * <p>
 * Provides standard CRUD via {@link JpaRepository} and custom queries
 * for active/inactive filtering, case-insensitive email uniqueness checks,
 * and partial-name search on active clientes.
 * </p>
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Checks whether a cliente with the given email already exists, ignoring case.
     *
     * @param correo the email to check
     * @return {@code true} if a cliente with that email exists
     */
    boolean existsByCorreoIgnoreCase(String correo);

    /**
     * Retrieves all active clientes sorted by name ascending.
     *
     * @return list of active clientes
     */
    List<Cliente> findByActivoTrueOrderByNombreAsc();

    /**
     * Performs a case-insensitive partial search on active clientes by name.
     *
     * @param nombre the search substring
     * @return list of matching active clientes sorted by name ascending
     */
    List<Cliente> findByNombreContainingIgnoreCaseAndActivoTrueOrderByNombreAsc(String nombre);

    /**
     * Retrieves all inactive (soft-deleted) clientes sorted by name ascending.
     *
     * @return list of inactive clientes
     */
    List<Cliente> findByActivoFalseOrderByNombreAsc();
}
