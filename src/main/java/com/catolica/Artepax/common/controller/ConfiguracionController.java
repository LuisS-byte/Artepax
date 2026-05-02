package com.catolica.Artepax.common.controller;

import com.catolica.Artepax.common.dto.ConfiguracionResponseDTO;
import com.catolica.Artepax.common.service.ConfiguracionService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracion")
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService; // P6: solo interfaz

    public ConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public List<ConfiguracionResponseDTO> listar() {
        return configuracionService.listar();
    }

    @GetMapping("/{clave}")
    public ResponseEntity<ConfiguracionResponseDTO> obtener(@PathVariable String clave) {
        return ResponseEntity.ok(configuracionService.obtener(clave));
    }

    @PutMapping("/{clave}")
    public ResponseEntity<ConfiguracionResponseDTO> actualizar(
            @PathVariable String clave,
            @RequestBody String nuevoValor) {
        String valor = nuevoValor.trim().replace("\"", "");
        return ResponseEntity.ok(configuracionService.actualizar(clave, valor));
    }
}
