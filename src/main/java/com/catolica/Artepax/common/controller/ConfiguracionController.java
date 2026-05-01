package com.catolica.Artepax.common.controller;

import com.catolica.Artepax.common.dto.ConfiguracionResponseDTO;
import com.catolica.Artepax.common.entity.Configuracion;
import com.catolica.Artepax.common.repository.ConfiguracionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracion")
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionController { private final ConfiguracionRepository repo;

    public ConfiguracionController(ConfiguracionRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ConfiguracionResponseDTO> listar() {
        return repo.findAll().stream().map(this::toDTO).toList();
    }

    @GetMapping("/{clave}")
    public ResponseEntity<ConfiguracionResponseDTO> obtener(@PathVariable String clave) {
        return repo.findById(clave)
                .map(c -> ResponseEntity.ok(toDTO(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{clave}")
    public ResponseEntity<ConfiguracionResponseDTO> actualizar(
            @PathVariable String clave,
            @RequestBody String nuevoValor) {

        return repo.findById(clave).map(c -> {
            String valor = nuevoValor.trim()
                    .replace("\"", ""); // quitar comillas si vienen del JSON

            // Validar según el tipo declarado en BD
            switch (c.getConfigTipo()) {
                case "INT" -> {
                    try {
                        Integer.parseInt(valor);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "La clave '" + clave + "' es de tipo INT. " +
                                        "Valor inválido: '" + valor + "'");
                    }
                }
                case "DECIMAL" -> {
                    try {
                        new java.math.BigDecimal(valor);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "La clave '" + clave + "' es de tipo DECIMAL. " +
                                        "Valor inválido: '" + valor + "'");
                    }
                }
                case "BOOLEAN" -> {
                    if (!valor.equalsIgnoreCase("true") && !valor.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException(
                                "La clave '" + clave + "' es de tipo BOOLEAN. " +
                                        "Valores permitidos: true, false");
                    }
                }
                // STRING — cualquier valor es válido, no requiere validación
            }

            c.setConfigValor(valor);
            return ResponseEntity.ok(toDTO(repo.save(c)));
        }).orElse(ResponseEntity.notFound().build());
    }
    private ConfiguracionResponseDTO toDTO(Configuracion c) {
        ConfiguracionResponseDTO dto = new ConfiguracionResponseDTO();
        dto.setConfigClave(c.getConfigClave());
        dto.setConfigValor(c.getConfigValor());
        dto.setConfigTipo(c.getConfigTipo());
        dto.setConfigDescripcion(c.getConfigDescripcion());
        return dto;
    }
}