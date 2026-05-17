package com.catolica.Artepax.pago.controller;

import com.catolica.Artepax.pago.dto.EventoCobranzaDTO;
import com.catolica.Artepax.pago.dto.PagoRequestDTO;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.dto.ResumenPagoEventoDTO;
import com.catolica.Artepax.pago.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return ResponseEntity.ok(pagoService.getResumenPagos(eventoId));
    }

    @GetMapping
    public ResponseEntity<Page<PagoResponseDTO>> obtenerTodos(
            @PageableDefault(size = 50, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagoService.obtenerTodos(pageable));
    }

    @GetMapping("/eventos-cobranza")
    public ResponseEntity<List<EventoCobranzaDTO>> getCobranza(
            @RequestParam(required = false) List<String> estado,
            @RequestParam(required = false) Boolean soloConSaldo,
            @RequestParam(required = false) Boolean soloConDevolucion,
            @RequestParam(required = false) Boolean soloConPagosPendientes,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit) {
        Map<String, Object> filtros = new HashMap<>();
        if (estado != null) filtros.put("estado", estado);
        if (soloConSaldo != null) filtros.put("soloConSaldo", soloConSaldo);
        if (soloConDevolucion != null) filtros.put("soloConDevolucion", soloConDevolucion);
        if (soloConPagosPendientes != null) filtros.put("soloConPagosPendientes", soloConPagosPendientes);
        if (q != null) filtros.put("q", q);
        if (limit != null) filtros.put("limit", limit);
        return ResponseEntity.ok(pagoService.getEventosCobranza(filtros));
    }
}