package com.catolica.Artepax.common.service.impl;

import com.catolica.Artepax.common.exception.ConfiguracionNotFoundException;
import com.catolica.Artepax.common.repository.ConfiguracionRepository;
import com.catolica.Artepax.common.service.ConfiguracionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.catolica.Artepax.common.dto.ConfiguracionResponseDTO;
import com.catolica.Artepax.common.entity.Configuracion;
import java.util.List;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfiguracionServiceImpl implements ConfiguracionService {

    private final ConfiguracionRepository repo;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public ConfiguracionServiceImpl(ConfiguracionRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    private String getRaw(String clave) {
        return cache.computeIfAbsent(clave, k ->
                repo.findById(k)
                        .orElseThrow(() -> new ConfiguracionNotFoundException(k))
                        .getConfigValor()
        );
    }

    public void invalidarCache(String clave) {
        cache.remove(clave);
    }

    @Override
    public String getString(String clave) { return getRaw(clave); }

    @Override
    public Integer getInt(String clave) {
        try {
            return Integer.parseInt(getRaw(clave));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Configuración '" + clave + "' no es un entero válido");
        }
    }

    @Override
    public BigDecimal getDecimal(String clave) {
        try {
            return new BigDecimal(getRaw(clave));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Configuración '" + clave + "' no es un decimal válido");
        }
    }

    @Override
    public Boolean getBoolean(String clave) {
        String valor = getRaw(clave);

        if ("true".equalsIgnoreCase(valor)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valor)) {
            return false;
        }

        throw new IllegalStateException(
                "La configuración '" + clave + "' no contiene un boolean válido. Valor actual: '" + valor + "'");
    }

    @Override
    public List<ConfiguracionResponseDTO> listar() {
        return repo.findAllByOrderByConfigClaveAsc().stream().map(this::toDTO).toList();
    }

    @Override
    public ConfiguracionResponseDTO obtener(String clave) {
        return repo.findById(clave)
                .map(this::toDTO)
                .orElseThrow(() -> new ConfiguracionNotFoundException(clave));
    }

    @Override
    public ConfiguracionResponseDTO actualizar(String clave, String valor) {
        Configuracion c = repo.findById(clave)
                .orElseThrow(() -> new ConfiguracionNotFoundException(clave));

        validarValorPorTipo(c.getConfigTipo(), valor);

        c.setConfigValor(valor);
        Configuracion guardada = repo.save(c);
        invalidarCache(clave);
        return toDTO(guardada);
    }

    private void validarValorPorTipo(String tipo, String valor) {
        switch (tipo) {
            case "INT" -> validarInt(valor);
            case "DECIMAL" -> validarDecimal(valor);
            case "BOOLEAN" -> validarBoolean(valor);
            case "JSON" -> validarJson(valor);
            case "STRING" -> { /* sin validación especial */ }
            default -> throw new IllegalArgumentException(
                    "Tipo de configuración no soportado: " + tipo);
        }
    }

    private void validarInt(String valor) {
        try {
            Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "La clave es de tipo INT. Valor inválido: '" + valor + "'");
        }
    }

    private void validarDecimal(String valor) {
        try {
            new BigDecimal(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "La clave es de tipo DECIMAL. Valor inválido: '" + valor + "'");
        }
    }

    private void validarBoolean(String valor) {
        if (!valor.equalsIgnoreCase("true") && !valor.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(
                    "La clave es de tipo BOOLEAN. Valores permitidos: true, false");
        }
    }

    private void validarJson(String valor) {
        try {
            objectMapper.readTree(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "El valor debe ser un JSON válido. Error: " + e.getMessage());
        }
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
