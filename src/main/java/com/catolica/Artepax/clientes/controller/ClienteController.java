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

@RestController
@RequestMapping("/api/clientes")
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

    // ✅ Historial de eventos por cliente
    @GetMapping("/{id}/eventos")
    public ResponseEntity<List<EventoResponseDTO>> obtenerEventosPorCliente(
            @PathVariable Long id) {

        List<EventoResponseDTO> eventos = clienteService.obtenerEventosPorCliente(id);
        return ResponseEntity.ok(eventos);
    }

    // GET /api/clientes/buscar?nombre=Juan
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponseDTO>> buscarPorNombre(
            @RequestParam String nombre) {

        List<ClienteResponseDTO> clientes = clienteService.buscarPorNombre(nombre);
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/inactivos")
    public ResponseEntity<List<ClienteResponseDTO>> listarInactivos() {
        return ResponseEntity.ok(clienteService.obtenerInactivos());
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<ClienteResponseDTO> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.reactivar(id));
    }
}
