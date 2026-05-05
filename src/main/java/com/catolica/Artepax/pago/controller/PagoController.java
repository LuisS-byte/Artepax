package com.catolica.Artepax.pago.controller;

import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;
import com.catolica.Artepax.pago.entity.VistaPagosEvento;
import com.catolica.Artepax.pago.repository.VistaPagosEventoRepository;
import com.catolica.Artepax.pago.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@PreAuthorize("hasRole('ADMIN')")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoResponseDTO> registrar(@Valid @RequestBody PagoRequestDTO dto) {
        return ResponseEntity.status(201).body(pagoService.registrarPago(dto));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<PagoResponseDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.confirmarPago(id));
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<PagoResponseDTO> anular(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.anularPago(id));
    }

    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<List<PagoResponseDTO>> porEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(pagoService.obtenerPorEvento(eventoId));
    }

    @GetMapping("/evento/{eventoId}/resumen")
    public ResponseEntity<ResumenPagoEventoDTO> getResumenPagos(@PathVariable Long eventoId) {
        return ResponseEntity.ok(pagoService.getResumenPagos(eventoId)); // P10
    }

    @GetMapping
    public ResponseEntity<Page<PagoResponseDTO>> obtenerTodos(
            @PageableDefault(size = 50, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagoService.obtenerTodos(pageable));
    }
}