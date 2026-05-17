package com.catolica.Artepax.inventario.decoracion.controller;


import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;
import com.catolica.Artepax.inventario.decoracion.service.DecoracionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for decoration package management.
 * <p>
 * Provides CRUD endpoints for decoration packages, including product association
 * management. All endpoints require the {@code ADMIN} role.
 * </p>
 */
@RestController
@RequestMapping("/api/decoraciones")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DecoracionController {
    private final DecoracionService decoracionService;

    /**
     * Creates a new decoration package with its product associations.
     *
     * @param dto the request body with decoration data and products
     * @return the created decoration with HTTP 201
     */
    @PostMapping
    public ResponseEntity<DecoracionResponseDTO> crearDecoracion(
            @Valid @RequestBody DecoracionRequestDTO dto) {

        DecoracionResponseDTO response = decoracionService.crearDecoracion(dto);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Retrieves all decoration packages, optionally filtered by state.
     *
     * @param estado optional state filter (e.g. ACTIVO, INACTIVO)
     * @return the list of decorations
     */
    @GetMapping
    public ResponseEntity<List<DecoracionResponseDTO>> obtenerTodas(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(decoracionService.obtenerTodas(estado));
    }

    /**
     * Retrieves a single decoration package by its ID.
     *
     * @param id the decoration ID
     * @return the decoration if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<DecoracionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(decoracionService.obtenerPorId(id));
    }

    /**
     * Updates an existing decoration package.
     *
     * @param id  the decoration ID to update
     * @param dto the request body with fields to update
     * @return the updated decoration
     */
    @PutMapping("/{id}")
    public ResponseEntity<DecoracionResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DecoracionUpdateDTO dto) {

        return ResponseEntity.ok(decoracionService.actualizar(id, dto));
    }

    /**
     * Soft-deletes a decoration package by setting its state to {@code INACTIVO}.
     *
     * @param id the decoration ID to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        decoracionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
