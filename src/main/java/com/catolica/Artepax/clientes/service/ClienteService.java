package com.catolica.Artepax.clientes.service;

import com.catolica.Artepax.clientes.dto.ClienteRequestDTO;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.dto.ClienteUpdateDTO;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;

import java.util.List;

/**
 * Service interface for cliente management.
 * <p>
 * Defines the contract for CRUD operations, soft-delete, reactivation, search,
 * and event history retrieval. All methods operate with {@code @Transactional}
 * semantics enforced by the implementation.
 * </p>
 */
public interface ClienteService {

    /**
     * Creates a new cliente from the given request data.
     * <p>
     * Normalises the email to lowercase, trims all text fields, and checks
     * for duplicate emails before persisting.
     * </p>
     *
     * @param dto the creation request data
     * @return the created cliente response
     * @throws IllegalArgumentException if the email is already in use
     */
    ClienteResponseDTO crearCliente(ClienteRequestDTO dto);

    /**
     * Retrieves all active clientes sorted by name ascending.
     *
     * @return list of active clientes
     */
    List<ClienteResponseDTO> obtenerTodos();

    /**
     * Retrieves all inactive (soft-deleted) clientes sorted by name ascending.
     *
     * @return list of inactive clientes
     */
    List<ClienteResponseDTO> obtenerInactivos();

    /**
     * Retrieves a cliente by its ID.
     *
     * @param id the cliente ID
     * @return the cliente response
     * @throws ClienteNotFoundException if no cliente with the given ID exists
     */
    ClienteResponseDTO obtenerPorId(Long id);

    /**
     * Updates an existing cliente.
     * <p>
     * Normalises the email and checks for uniqueness if the email has changed.
     * </p>
     *
     * @param id  the ID of the cliente to update
     * @param dto the update data
     * @return the updated cliente response
     * @throws ClienteNotFoundException if no cliente with the given ID exists
     * @throws IllegalArgumentException if the new email is already in use by another cliente
     */
    ClienteResponseDTO actualizar(Long id, ClienteUpdateDTO dto);

    /**
     * Soft-deletes a cliente by setting its active flag to {@code false}.
     * <p>
     * The cliente's event history is preserved and remains accessible.
     * </p>
     *
     * @param id the ID of the cliente to deactivate
     * @throws ClienteNotFoundException if no cliente with the given ID exists
     */
    void eliminar(Long id);

    /**
     * Reactivates a previously soft-deleted cliente.
     *
     * @param id the ID of the cliente to reactivate
     * @return the reactivated cliente response
     * @throws ClienteNotFoundException if no cliente with the given ID exists
     */
    ClienteResponseDTO reactivar(Long id);

    /**
     * Searches for active clientes whose name contains the given string (case-insensitive).
     *
     * @param nombre the search substring
     * @return list of matching active clientes sorted by name ascending
     */
    List<ClienteResponseDTO> buscarPorNombre(String nombre);

    /**
     * Retrieves the event history for a specific cliente.
     *
     * @param clienteId the cliente ID
     * @return list of eventos associated with the cliente
     * @throws ClienteNotFoundException if no cliente with the given ID exists
     */
    List<EventoResponseDTO> obtenerEventosPorCliente(Long clienteId);
}