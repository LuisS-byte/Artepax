package com.catolica.Artepax.pago.controller;

import com.catolica.Artepax.pago.dto.*;
import com.catolica.Artepax.pago.service.CatalogosPagoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing payment catalog endpoints.
 * <p>
 * Provides read access to payment states, concepts, and methods,
 * and write operations for managing payment methods (create, toggle).
 * All endpoints require {@code ADMIN} role.
 */
@RestController
@RequestMapping("/api/catalogos")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogosPagoController {

    private final CatalogosPagoService service;

    public CatalogosPagoController(CatalogosPagoService service) {
        this.service = service;
    }

    /**
     * Lists all payment states (PENDIENTE, PAGADO, CANCELADO, DEVOLUCION_PENDIENTE).
     *
     * @return list of payment state DTOs
     */
    @GetMapping("/estados-pago")
    public List<EstadoPagoResponseDTO> listarEstados() {
        return service.listarEstados();
    }

    /**
     * Lists all payment concepts (ANTICIPO, ABONO, SALDO_TOTAL, PENALIZACION).
     *
     * @return list of payment concept DTOs
     */
    @GetMapping("/conceptos-pago")
    public List<ConceptoPagoResponseDTO> listarConceptos() {
        return service.listarConceptos();
    }

    /**
     * Lists all payment methods.
     *
     * @return list of payment method DTOs
     */
    @GetMapping("/metodos-pago")
    public List<MetodoPagoResponseDTO> listarMetodos() {
        return service.listarMetodos();
    }

    /**
     * Creates a new payment method.
     *
     * @param body the request body with nombre and optional descripcion
     * @return 200 OK with the created payment method
     */
    @PostMapping("/metodos-pago")
    public ResponseEntity<MetodoPagoResponseDTO> crearMetodo(@Valid @RequestBody MetodoPagoCreateDTO body) {
        return ResponseEntity.ok(service.crearMetodo(
                body.getNombre(),
                body.getDescripcion()));
    }

    /**
     * Toggles the active state of a payment method (soft-delete).
     *
     * @param id the payment method ID
     * @return 200 OK with the updated payment method
     */
    @PatchMapping("/metodos-pago/{id}/toggle")
    public ResponseEntity<MetodoPagoResponseDTO> toggleMetodo(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleMetodo(id));
    }
}
