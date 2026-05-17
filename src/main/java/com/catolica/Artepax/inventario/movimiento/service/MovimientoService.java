package com.catolica.Artepax.inventario.movimiento.service;

import com.catolica.Artepax.inventario.movimiento.dto.AjusteInventarioRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.StockProductoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for inventory movement operations.
 * Defines the contract for stock entries (REPOSICION, AJUSTE_MANUAL), exits
 * (DAÑO, PERDIDA, AJUSTE_MANUAL), physical-count adjustments, stock queries,
 * and movement history retrieval.
 */
public interface MovimientoService {

    /**
     * Registers a stock entry movement (REPOSICION or AJUSTE_MANUAL).
     * Increases the product's total quantity and records the movement.
     *
     * @param dto the movement request with product ID, quantity, and origin
     */
    void registrarEntrada(MovimientoRequestDTO dto);

    /**
     * Registers a stock exit movement (DAÑO, PERDIDA, or AJUSTE_MANUAL).
     * Decreases the product's total quantity after validating available stock.
     *
     * @param dto the movement request with product ID, quantity, and origin
     */
    void registrarSalida(MovimientoRequestDTO dto);

    /**
     * Adjusts inventory based on a physical count.
     * Calculates the difference between current system quantity and the observed
     * physical count, then creates the appropriate ENTRADA or SALIDA movement.
     * Validates that the physical count does not fall below active reservations.
     *
     * @param productoId the product ID to adjust
     * @param request    the physical count and reason
     */
    void registrarAjuste(Long productoId, AjusteInventarioRequestDTO request);

    /**
     * Retrieves the current stock information for a product from the stock view.
     *
     * @param productoId the product ID
     * @return the stock information DTO
     */
    StockProductoResponseDTO obtenerStock(Long productoId);

    /**
     * Returns a paginated history of movements for a specific product.
     *
     * @param productoId the product ID
     * @param pageable   pagination parameters
     * @return a page of movement DTOs
     */
    Page<MovimientoResponseDTO> obtenerMovimientosPorProducto(Long productoId, Pageable pageable);

    /**
     * Returns a paginated history of all inventory movements across all products.
     *
     * @param pageable pagination parameters
     * @return a page of movement DTOs
     */
    Page<MovimientoResponseDTO> obtenerTodosMovimientos(Pageable pageable);
}
