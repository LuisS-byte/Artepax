package com.catolica.Artepax.pago.controller;

import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.service.PagoService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pagos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

// GET /pagos/evento/{eventoId}
@GetMapping("/evento/{eventoId}")
public ResponseEntity<List<PagoResponseDTO>> obtenerPorEvento(
        @PathVariable Long eventoId) {
    return ResponseEntity.ok(pagoService.obtenerPorEvento(eventoId));
}

// PUT /pagos/evento/{eventoId}/confirmar
@PutMapping("/evento/{eventoId}/confirmar")
public ResponseEntity<PagoResponseDTO> confirmarPago(
        @PathVariable Long eventoId) {
    return ResponseEntity.ok(pagoService.confirmarPago(eventoId));
}
}