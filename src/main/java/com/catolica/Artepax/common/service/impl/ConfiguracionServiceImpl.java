package com.catolica.Artepax.common.service.impl;

import com.catolica.Artepax.common.exception.ConfiguracionNotFoundException;
import com.catolica.Artepax.common.repository.ConfiguracionRepository;
import com.catolica.Artepax.common.service.ConfiguracionService;
import org.springframework.stereotype.Service;
import com.catolica.Artepax.common.dto.ConfiguracionResponseDTO;
import com.catolica.Artepax.common.entity.Configuracion;
import java.util.List;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfiguracionServiceImpl implements ConfiguracionService {


    private final ConfiguracionRepository repo;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>(); // P11

    public ConfiguracionServiceImpl(ConfiguracionRepository repo) {
        this.repo = repo;
    }

    private String getRaw(String clave) {
        return cache.computeIfAbsent(clave, k ->          // P11: hit = retorna del cache, miss = query a BD
                repo.findById(k)
                        .orElseThrow(() -> new ConfiguracionNotFoundException(k))
                        .getConfigValor()
        );
    }

    public void invalidarCache(String clave) { // P11: llamar desde ConfiguracionController al hacer PUT
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
        return Boolean.parseBoolean(getRaw(clave));
    }

    @Override
    public List<ConfiguracionResponseDTO> listar() {
        return repo.findAll().stream().map(this::toDTO).toList();
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

        switch (c.getConfigTipo()) {
            case "INT" -> {
                try { Integer.parseInt(valor); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "La clave '" + clave + "' es de tipo INT. Valor inválido: '" + valor + "'");
                }
            }
            case "DECIMAL" -> {
                try { new java.math.BigDecimal(valor); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "La clave '" + clave + "' es de tipo DECIMAL. Valor inválido: '" + valor + "'");
                }
            }
            case "BOOLEAN" -> {
                if (!valor.equalsIgnoreCase("true") && !valor.equalsIgnoreCase("false"))
                    throw new IllegalArgumentException(
                            "La clave '" + clave + "' es de tipo BOOLEAN. Valores permitidos: true, false");
            }
        }

        c.setConfigValor(valor);
        Configuracion guardada = repo.save(c);
        invalidarCache(clave);
        return toDTO(guardada);
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
