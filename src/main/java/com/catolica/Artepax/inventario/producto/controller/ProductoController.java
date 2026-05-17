package com.catolica.Artepax.inventario.producto.controller;

import com.catolica.Artepax.inventario.producto.dto.ProductoRequestDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoResponseDTO;
import com.catolica.Artepax.inventario.producto.dto.ProductoUpdateDTO;
import com.catolica.Artepax.inventario.producto.service.ProductoService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for product management.
 * <p>
 * Provides CRUD endpoints for inventory products, including search by name
 * and state transitions (soft-delete / reactivate). All endpoints require
 * the {@code ADMIN} role.
 * </p>
 */
@RestController
@RequestMapping("/api/productos")
@PreAuthorize("hasRole('ADMIN')")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * Creates a new product.
     *
     * @param requestDTO the request body with product data
     * @return the created product with HTTP 201
     */
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crearProducto(
            @Valid @RequestBody ProductoRequestDTO requestDTO) {

        ProductoResponseDTO producto = productoService.crearProducto(requestDTO);
        return ResponseEntity.status(201).body(producto);
    }

    /**
     * Retrieves all products.
     *
     * @return the list of all products
     */
    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> obtenerTodos() {
        List<ProductoResponseDTO> productos = productoService.obtenerTodos();
        return ResponseEntity.ok(productos);
    }

    /**
     * Retrieves a single product by its ID.
     *
     * @param id the product ID
     * @return the product if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(
            @PathVariable Long id) {

        ProductoResponseDTO producto = productoService.obtenerPorId(id);
        return ResponseEntity.ok(producto);
    }

    /**
     * Updates an existing product with the given data.
     *
     * @param id       the product ID to update
     * @param updateDTO the request body with fields to update
     * @return the updated product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUpdateDTO updateDTO) {

        ProductoResponseDTO actualizado = productoService.actualizar(id, updateDTO);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Searches for products whose name contains the given string (case-insensitive).
     *
     * @param nombre the search string
     * @return the list of matching products
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ProductoResponseDTO>> buscarPorNombre(
            @RequestParam String nombre) {

        List<ProductoResponseDTO> productos = productoService.buscarPorNombre(nombre);
        return ResponseEntity.ok(productos);
    }

    /**
     * Soft-deletes a product by setting its state to {@code INACTIVO}.
     *
     * @param id the product ID to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a previously deactivated product.
     *
     * @param id the product ID to reactivate
     * @return the reactivated product
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<ProductoResponseDTO> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.reactivar(id));
    }
}