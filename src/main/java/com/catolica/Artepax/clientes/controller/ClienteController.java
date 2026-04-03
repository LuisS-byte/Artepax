package com.catolica.Artepax.clientes.controller;

import com.catolica.Artepax.clientes.dto.ClienteRequestDTO;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.dto.ClienteUpdateDTO;
import com.catolica.Artepax.clientes.service.ClienteService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/clientes")
@PreAuthorize("hasRole('ADMIN')")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // ✅ Crear cliente
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> crearCliente(
            @Valid @RequestBody ClienteRequestDTO requestDTO) {

        ClienteResponseDTO cliente = clienteService.crearCliente(requestDTO);
        return ResponseEntity.status(201).body(cliente);
    }

    // ✅ Obtener todos los clientes
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> obtenerTodos() {
        List<ClienteResponseDTO> clientes = clienteService.obtenerTodos();
        return ResponseEntity.ok(clientes);
    }

    // ✅ Obtener cliente por ID
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> obtenerPorId(
            @PathVariable Long id) {

        ClienteResponseDTO cliente = clienteService.obtenerPorId(id);
        return ResponseEntity.ok(cliente);
    }

    // ✅ Actualizar cliente
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateDTO updateDTO) {

        ClienteResponseDTO actualizado = clienteService.actualizar(id, updateDTO);
        return ResponseEntity.ok(actualizado);
    }

    // ✅ Eliminar cliente
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(
            @PathVariable Long id) {

        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}