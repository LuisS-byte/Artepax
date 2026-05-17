package com.catolica.Artepax.inventario.producto.repository;

import com.catolica.Artepax.inventario.producto.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Producto} entities.
 * Provides standard CRUD operations plus custom queries with pessimistic locking
 * for concurrent stock management.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Finds a product by its exact name.
     *
     * @param nombre the product name to search for
     * @return an {@link Optional} containing the product if found
     */
    Optional<Producto> findByNombre(String nombre);

    /**
     * Finds a product by its name, ignoring case.
     *
     * @param nombre the product name to search for
     * @return an {@link Optional} containing the product if found
     */
    Optional<Producto> findByNombreIgnoreCase(String nombre);

    /**
     * Checks whether a product with the given name already exists.
     *
     * @param nombre the product name to check
     * @return {@code true} if a product with that name exists
     */
    boolean existsByNombre(String nombre);

    /**
     * Finds all products whose name contains the given string, ignoring case.
     *
     * @param nombre the search string
     * @return the list of matching products
     */
    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Finds all products by their IDs using {@link LockModeType#PESSIMISTIC_WRITE}.
     * <p>
     * Acquires a database-level row lock ({@code SELECT ... FOR UPDATE}) on the matching
     * rows to prevent concurrent modifications during stock operations. IDs are expected
     * to be sorted before calling this method to avoid deadlocks.
     * </p>
     *
     * @param ids the collection of product IDs to lock and retrieve
     * @return the list of locked products
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Producto p
            WHERE p.id IN :ids
            """)
    List<Producto> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    /**
     * Finds a single product by ID with a {@link LockModeType#PESSIMISTIC_WRITE} lock.
     * <p>
     * Acquires a row-level exclusive lock ({@code SELECT ... FOR UPDATE}) to prevent
     * concurrent stock updates on the same product.
     * </p>
     *
     * @param id the product ID to lock and retrieve
     * @return an {@link Optional} containing the locked product if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
