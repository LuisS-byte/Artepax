package com.catolica.Artepax.inventario.decoracion.service;


import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;

import java.util.List;
/**
 * Service interface for decoration package operations.
 * Defines the contract for CRUD on decoration packages, including
 * product association management.
 */
public interface DecoracionService {

    /**
     * Creates a new decoration package with its product associations.
     * Validates product stock availability and name uniqueness.
     *
     * @param dto the request data with decoration fields and products
     * @return the created decoration as a response DTO
     */
    DecoracionResponseDTO crearDecoracion(DecoracionRequestDTO dto);

    /**
     * Retrieves all decoration packages, optionally filtered by state.
     *
     * @param estado optional state filter (e.g. ACTIVO, INACTIVO); if null, returns all
     * @return the list of decoration DTOs with their products
     */
    List<DecoracionResponseDTO> obtenerTodas(String estado);

    /**
     * Retrieves a single decoration package by its ID, including its products.
     *
     * @param id the decoration ID
     * @return the decoration as a response DTO
     * @throws DecoracionNotFoundException if not found
     */
    DecoracionResponseDTO obtenerPorId(Long id);

    /**
     * Updates an existing decoration package. If products are provided in the DTO,
     * existing associations are replaced entirely.
     *
     * @param id  the decoration ID to update
     * @param dto the data to apply
     * @return the updated decoration as a response DTO
     */
    DecoracionResponseDTO actualizar(Long id, DecoracionUpdateDTO dto);

    /**
     * Soft-deletes a decoration package by setting its state to {@code INACTIVO}.
     *
     * @param id the decoration ID to deactivate
     */
    void eliminar(Long id);
}
