package com.catolica.Artepax.pago.controller;

import com.catolica.Artepax.pago.dto.*;
import com.catolica.Artepax.pago.service.CatalogosPagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogos")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogosPagoController {

    private final CatalogosPagoService service;

    public CatalogosPagoController(CatalogosPagoService service) {
        this.service = service;
    }

    // --- EstadoPago ---
    @GetMapping("/estados-pago")
    public List<EstadoPagoResponseDTO> listarEstados() {
        return service.listarEstados();
    }

    // --- ConceptoPago ---
    @GetMapping("/conceptos-pago")
    public List<ConceptoPagoResponseDTO> listarConceptos() {
        return service.listarConceptos();
    }

    // --- MetodoPago ---
    @GetMapping("/metodos-pago")
    public List<MetodoPagoResponseDTO> listarMetodos() {
        return service.listarMetodos();
    }

    @PostMapping("/metodos-pago")
    public ResponseEntity<MetodoPagoResponseDTO> crearMetodo(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.crearMetodo(
                body.get("nombre"),
                body.get("descripcion")));
    }

    @PatchMapping("/metodos-pago/{id}/toggle")
    public ResponseEntity<MetodoPagoResponseDTO> toggleMetodo(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleMetodo(id));
    }
}