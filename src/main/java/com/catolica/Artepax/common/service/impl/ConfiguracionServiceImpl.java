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

/**
 * Implementación de {@link ConfiguracionService} con caché en memoria.
 * <p>
 * Usa un {@link ConcurrentHashMap} como caché de valores crudos (String).
 * Los accessors tipados ({@link #getInt}, {@link #getDecimal}, {@link #getBoolean})
 * parsean el valor cacheado y lanzan {@link IllegalStateException} si la
 * conversión falla.
 * <p>
 * <strong>Importante:</strong> después de actualizar un valor mediante
 * {@link #actualizar(String, String)}, el caché se invalida automáticamente.
 * Si se modifica la base de datos por fuera (ej. script directo), se debe
 * llamar a {@link #invalidarCache(String)} para evitar lecturas stale.
 * <p>
 * Los métodos {@link #listar()} y {@link #obtener(String)} leen directamente
 * de la base de datos (no del caché) para garantizar datos frescos en las
 * operaciones CRUD.
 */
@Service
public class ConfiguracionServiceImpl implements ConfiguracionService {

    private final ConfiguracionRepository repo;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public ConfiguracionServiceImpl(ConfiguracionRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    /**
     * Obtiene el valor crudo de una clave desde el caché, cargándolo desde la
     * base de datos si no está presente (cache-aside pattern).
     *
     * @param clave la clave de configuración
     * @return el valor textual almacenado
     * @throws ConfiguracionNotFoundException si la clave no existe en la BD
     */
    private String getRaw(String clave) {
        return cache.computeIfAbsent(clave, k ->
                repo.findById(k)
                        .orElseThrow(() -> new ConfiguracionNotFoundException(k))
                        .getConfigValor()
        );
    }

    /**
     * Invalida la entrada en caché para una clave específica.
     * La próxima lectura recargará el valor desde la base de datos.
     *
     * @param clave la clave de configuración a invalidar
     */
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

    /**
     * Valida que el valor sea compatible con el tipo de configuración.
     *
     * @param tipo  el tipo esperado (INT, DECIMAL, BOOLEAN, JSON, STRING)
     * @param valor el valor a validar
     * @throws IllegalArgumentException si el valor no es válido para el tipo
     */
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

    /**
     * Valida que el valor sea un entero válido.
     *
     * @param valor el valor a validar
     * @throws IllegalArgumentException si no es un entero
     */
    private void validarInt(String valor) {
        try {
            Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "La clave es de tipo INT. Valor inválido: '" + valor + "'");
        }
    }

    /**
     * Valida que el valor sea un decimal válido.
     *
     * @param valor el valor a validar
     * @throws IllegalArgumentException si no es un decimal
     */
    private void validarDecimal(String valor) {
        try {
            new BigDecimal(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "La clave es de tipo DECIMAL. Valor inválido: '" + valor + "'");
        }
    }

    /**
     * Valida que el valor sea "true" o "false" (case-insensitive).
     *
     * @param valor el valor a validar
     * @throws IllegalArgumentException si no es un booleano válido
     */
    private void validarBoolean(String valor) {
        if (!valor.equalsIgnoreCase("true") && !valor.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(
                    "La clave es de tipo BOOLEAN. Valores permitidos: true, false");
        }
    }

    /**
     * Valida que el valor sea un JSON válido.
     *
     * @param valor el valor a validar
     * @throws IllegalArgumentException si no es un JSON válido
     */
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
