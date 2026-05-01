package com.catolica.Artepax.inventario.decoracion.controller;


import com.catolica.Artepax.inventario.decoracion.dto.DecoracionRequestDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionResponseDTO;
import com.catolica.Artepax.inventario.decoracion.dto.DecoracionUpdateDTO;
import com.catolica.Artepax.inventario.decoracion.service.DecoracionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decoraciones")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DecoracionController {private final DecoracionService decoracionService;

    @PostMapping
    public ResponseEntity<DecoracionResponseDTO> crearDecoracion(
            @Valid @RequestBody DecoracionRequestDTO dto) {

        DecoracionResponseDTO response = decoracionService.crearDecoracion(dto);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DecoracionResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(decoracionService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecoracionResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(decoracionService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DecoracionResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DecoracionUpdateDTO dto) {

        return ResponseEntity.ok(decoracionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        decoracionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
