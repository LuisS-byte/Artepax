package com.catolica.Artepax.evento.controller;

import com.catolica.Artepax.evento.dto.TipoEventoResponseDTO;
import com.catolica.Artepax.evento.service.TipoEventoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tipos-evento")
@PreAuthorize("hasRole('ADMIN')")
public class TipoEventoController {
    private final TipoEventoService service;

    public TipoEventoController(TipoEventoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TipoEventoResponseDTO> listar() {
        return service.listar();
    }

    @PostMapping
    public ResponseEntity<TipoEventoResponseDTO> crear(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.crear(body.get("nombre"), body.get("descripcion")));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<TipoEventoResponseDTO> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggle(id));
    }
}

