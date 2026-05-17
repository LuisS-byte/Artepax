package com.catolica.Artepax.clientes.controller;

import com.catolica.Artepax.clientes.dto.ClienteRequestDTO;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.dto.ClienteUpdateDTO;
import com.catolica.Artepax.clientes.service.ClienteService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for managing clientes.
 * <p>
 * Exposes CRUD endpoints, soft-delete (deactivation), reactivation, search,
 * and event history lookup for clientes. All endpoints require the {@code ADMIN} role.
 * </p>
 */
@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasRole('ADMIN')")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * Creates a new cliente.
     *
     * @param requestDTO the cliente creation data
     * @return the created cliente with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crearCliente(
            @Valid @RequestBody ClienteRequestDTO requestDTO) {

        ClienteResponseDTO cliente = clienteService.crearCliente(requestDTO);
        return ResponseEntity.status(201).body(cliente);
    }

    /**
     * Retrieves all active clientes sorted by name.
     *
     * @return list of active clientes
     */
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> obtenerTodos() {
        List<ClienteResponseDTO> clientes = clienteService.obtenerTodos();
        return ResponseEntity.ok(clientes);
    }

    /**
     * Retrieves a cliente by its ID.
     *
     * @param id the cliente ID
     * @return the cliente if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorId(
            @PathVariable Long id) {

        ClienteResponseDTO cliente = clienteService.obtenerPorId(id);
        return ResponseEntity.ok(cliente);
    }

    /**
     * Updates an existing cliente.
     *
     * @param id        the ID of the cliente to update
     * @param updateDTO the update data
     * @return the updated cliente
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateDTO updateDTO) {

        ClienteResponseDTO actualizado = clienteService.actualizar(id, updateDTO);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Soft-deletes a cliente by setting it as inactive.
     *
     * @param id the ID of the cliente to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(
            @PathVariable Long id) {

        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the event history for a specific cliente.
     *
     * @param id the cliente ID
     * @return list of eventos associated with the cliente
     */
    @GetMapping("/{id}/eventos")
    public ResponseEntity<List<EventoResponseDTO>> obtenerEventosPorCliente(
            @PathVariable Long id) {

        List<EventoResponseDTO> eventos = clienteService.obtenerEventosPorCliente(id);
        return ResponseEntity.ok(eventos);
    }

    /**
     * Searches for active clientes whose name contains the given string (case-insensitive).
     *
     * @param nombre the search string
     * @return list of matching clientes sorted by name
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponseDTO>> buscarPorNombre(
            @RequestParam String nombre) {

        List<ClienteResponseDTO> clientes = clienteService.buscarPorNombre(nombre);
        return ResponseEntity.ok(clientes);
    }

    /**
     * Retrieves all inactive (soft-deleted) clientes sorted by name.
     *
     * @return list of inactive clientes
     */
    @GetMapping("/inactivos")
    public ResponseEntity<List<ClienteResponseDTO>> listarInactivos() {
        return ResponseEntity.ok(clienteService.obtenerInactivos());
    }

    /**
     * Reactivates a previously soft-deleted cliente.
     *
     * @param id the ID of the cliente to reactivate
     * @return the reactivated cliente
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<ClienteResponseDTO> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.reactivar(id));
    }
}
