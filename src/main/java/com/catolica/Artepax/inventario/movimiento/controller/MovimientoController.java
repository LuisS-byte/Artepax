package com.catolica.Artepax.inventario.movimiento.controller;

import com.catolica.Artepax.inventario.movimiento.dto.AjusteInventarioRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoRequestDTO;
import com.catolica.Artepax.inventario.movimiento.dto.MovimientoResponseDTO;
import com.catolica.Artepax.inventario.movimiento.service.MovimientoService;
import com.catolica.Artepax.inventario.producto.dto.StockProductoResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for inventory management operations.
 * <p>
 * Provides endpoints for stock entries, exits, physical-count adjustments,
 * stock queries, and movement history. All endpoints require the {@code ADMIN} role.
 * </p>
 */
@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasRole('ADMIN')")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    /**
     * Registers a stock entry movement (REPOSICION or AJUSTE_MANUAL).
     *
     * @param dto the movement request with product ID, quantity, and origin
     * @return HTTP 204 No Content
     */
    @PostMapping("/entrada")
    public ResponseEntity<Void> entrada(@Valid @RequestBody MovimientoRequestDTO dto) {
        movimientoService.registrarEntrada(dto);
        return ResponseEntity.noContent().build();
    }

    /**
     * Registers a stock exit movement (DAÑO, PERDIDA, or AJUSTE_MANUAL).
     *
     * @param dto the movement request with product ID, quantity, and origin
     * @return HTTP 204 No Content
     */
    @PostMapping("/salida")
    public ResponseEntity<Void> salida(@Valid @RequestBody MovimientoRequestDTO dto) {
        movimientoService.registrarSalida(dto);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adjusts stock based on a physical count for the given product.
     * Calculates the difference automatically and creates the required movement.
     *
     * @param productoId the product ID to adjust
     * @param request    the observed physical quantity and reason
     * @return HTTP 204 No Content
     */
    @PostMapping("/ajuste/{productoId}")
    public ResponseEntity<Void> ajustarStock(
            @PathVariable Long productoId,
            @Valid @RequestBody AjusteInventarioRequestDTO request) {
        movimientoService.registrarAjuste(productoId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves current stock information for a product from the stock view.
     *
     * @param id the product ID
     * @return the stock information DTO
     */
    @GetMapping("/productos/{id}/stock")
    public ResponseEntity<StockProductoResponseDTO> obtenerStock(@PathVariable Long id) {
        return ResponseEntity.ok(movimientoService.obtenerStock(id));
    }

    /**
     * Returns a paginated history of movements for a specific product.
     *
     * @param id       the product ID
     * @param pageable pagination and sorting parameters (default: size 50, sorted by fecha DESC)
     * @return a page of movement DTOs
     */
    @GetMapping("/productos/{id}/movimientos")
    public ResponseEntity<Page<MovimientoResponseDTO>> obtenerMovimientos(
            @PathVariable Long id,
            @PageableDefault(size = 50, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(movimientoService.obtenerMovimientosPorProducto(id, pageable));
    }

    /**
     * Returns a paginated history of all inventory movements across all products.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of movement DTOs
     */
    @GetMapping("/movimientos")
    public ResponseEntity<Page<MovimientoResponseDTO>> obtenerTodosMovimientos(
            @PageableDefault(size = 50, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(movimientoService.obtenerTodosMovimientos(pageable));
    }
}