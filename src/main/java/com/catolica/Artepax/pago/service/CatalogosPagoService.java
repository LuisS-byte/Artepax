package com.catolica.Artepax.pago.service;

import com.catolica.Artepax.pago.dto.ConceptoPagoResponseDTO;
import com.catolica.Artepax.pago.dto.EstadoPagoResponseDTO;
import com.catolica.Artepax.pago.dto.MetodoPagoResponseDTO;

import java.util.List;

/**
 * Service interface for payment catalogs (estados, conceptos, metodos).
 * <p>
 * Provides read operations for catalog listings and write operations
 * for managing payment methods (create, toggle active state).
 */
public interface CatalogosPagoService {

    /**
     * Returns all payment states (PENDIENTE, PAGADO, CANCELADO, DEVOLUCION_PENDIENTE).
     *
     * @return list of payment state DTOs
     */
    List<EstadoPagoResponseDTO> listarEstados();

    /**
     * Returns all payment concepts (ANTICIPO, ABONO, SALDO_TOTAL, PENALIZACION).
     *
     * @return list of payment concept DTOs
     */
    List<ConceptoPagoResponseDTO> listarConceptos();

    /**
     * Returns all payment methods.
     *
     * @return list of payment method DTOs
     */
    List<MetodoPagoResponseDTO> listarMetodos();

    /**
     * Creates a new payment method with the given name and optional description.
     * Validates for duplicates (case-insensitive) before persisting.
     *
     * @param nombre      the payment method name (must be unique)
     * @param descripcion optional description
     * @return the created payment method DTO
     */
    MetodoPagoResponseDTO crearMetodo(String nombre, String descripcion);

    /**
     * Toggles the active flag of a payment method (soft-delete).
     *
     * @param id the payment method ID
     * @return the updated payment method DTO
     */
    MetodoPagoResponseDTO toggleMetodo(Long id);
}