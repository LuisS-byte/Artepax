package com.catolica.Artepax.common.service.impl;

import com.catolica.Artepax.common.repository.ConfiguracionRepository;
import com.catolica.Artepax.common.service.ConfiguracionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ConfiguracionServiceImpl implements ConfiguracionService { private final ConfiguracionRepository repo;

    public ConfiguracionServiceImpl(ConfiguracionRepository repo) {
        this.repo = repo;
    }

    private String getRaw(String clave) {
        return repo.findById(clave)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuración no encontrada: " + clave))
                .getConfigValor();
    }

    @Override
    public String getString(String clave) {
        return getRaw(clave);
    }

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
}