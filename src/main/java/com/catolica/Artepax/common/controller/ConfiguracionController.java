package com.catolica.Artepax.common.controller;

import com.catolica.Artepax.common.dto.ConfiguracionResponseDTO;
import com.catolica.Artepax.common.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller para gestión de configuración global del sistema.
 */
@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<List<ConfiguracionResponseDTO>> listar() {
        return ResponseEntity.ok(configuracionService.listar());
    }

    @GetMapping("/{clave}")
    public ResponseEntity<ConfiguracionResponseDTO> obtener(@PathVariable String clave) {
        return ResponseEntity.ok(configuracionService.obtener(clave));
    }

    @PutMapping("/{clave}")
    public ResponseEntity<ConfiguracionResponseDTO> actualizar(
            @PathVariable String clave,
            @RequestBody String nuevoValor) {
        String valor = normalizarBodyString(nuevoValor);
        log.info("Actualizando configuración: clave={}", clave);
        return ResponseEntity.ok(configuracionService.actualizar(clave, valor));
    }

    /**
     * Normaliza el body string plano recibido en PUT.
     * Solo quita comillas envolventes si el valor completo está entre comillas dobles.
     * No elimina comillas internas para no destruir JSON.
     */
    private String normalizarBodyString(String nuevoValor) {
        if (nuevoValor == null) {
            return null;
        }

        String valor = nuevoValor.trim();

        if (valor.length() >= 2 && valor.startsWith("\"") && valor.endsWith("\"")) {
            valor = valor.substring(1, valor.length() - 1);
        }

        return valor;
    }
}
