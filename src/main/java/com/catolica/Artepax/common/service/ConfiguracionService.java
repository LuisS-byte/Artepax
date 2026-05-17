package com.catolica.Artepax.common.service;



import com.catolica.Artepax.common.dto.ConfiguracionResponseDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de acceso a configuración global del sistema.
 * <p>
 * Los valores se almacenan en la base de datos (tabla {@code configuracion})
 * y se cachean en memoria. Después de actualizar un valor mediante
 * {@link #actualizar(String, String)}, el caché se invalida automáticamente.
 * Si se modifica la base de datos por fuera de este servicio, se debe llamar
 * a {@link #invalidarCache(String)} para evitar lecturas stale.
 * <p>
 * Provee accessors tipados ({@link #getString}, {@link #getInt},
 * {@link #getDecimal}, {@link #getBoolean}) que lanzan
 * {@link IllegalStateException} si el valor no puede convertirse al tipo
 * solicitado.
 */
public interface ConfiguracionService {

    /**
     * Retorna el valor de configuración como String.
     *
     * @param clave la clave de configuración
     * @return el valor textual
     * @throws com.catolica.Artepax.common.exception.ConfiguracionNotFoundException si la clave no existe
     */
    String getString(String clave);

    /**
     * Retorna el valor de configuración como Integer.
     *
     * @param clave la clave de configuración
     * @return el valor convertido a entero
     * @throws com.catolica.Artepax.common.exception.ConfiguracionNotFoundException si la clave no existe
     * @throws IllegalStateException si el valor no es un entero válido
     */
    Integer getInt(String clave);

    /**
     * Retorna el valor de configuración como BigDecimal.
     *
     * @param clave la clave de configuración
     * @return el valor convertido a decimal
     * @throws com.catolica.Artepax.common.exception.ConfiguracionNotFoundException si la clave no existe
     * @throws IllegalStateException si el valor no es un decimal válido
     */
    BigDecimal getDecimal(String clave);

    /**
     * Retorna el valor de configuración como Boolean.
     *
     * @param clave la clave de configuración
     * @return el valor convertido a booleano ({@code "true"}/{@code "false"})
     * @throws com.catolica.Artepax.common.exception.ConfiguracionNotFoundException si la clave no existe
     * @throws IllegalStateException si el valor no es "true" o "false" (case-insensitive)
     */
    Boolean getBoolean(String clave);

    /**
     * Lista todas las claves de configuración ordenadas alfabéticamente.
     *
     * @return lista de DTOs con todas las configuraciones
     */
    List<ConfiguracionResponseDTO> listar();

    /**
     * Obtiene una configuración por su clave.
     *
     * @param clave la clave de configuración
     * @return el DTO con los datos de la configuración
     * @throws com.catolica.Artepax.common.exception.ConfiguracionNotFoundException si la clave no existe
     */
    ConfiguracionResponseDTO obtener(String clave);

    /**
     * Actualiza el valor de una configuración existente.
     * <p>
     * Valida el nuevo valor contra el tipo de la configuración
     * (INT, DECIMAL, BOOLEAN, JSON, STRING). Invalida automáticamente
     * el caché después de guardar.
     *
     * @param clave la clave de configuración a actualizar
     * @param valor el nuevo valor (en texto plano)
     * @return el DTO con los datos actualizados
     * @throws com.catolica.Artepax.common.exception.ConfiguracionNotFoundException si la clave no existe
     * @throws IllegalArgumentException si el valor no es válido para el tipo de la clave
     */
    ConfiguracionResponseDTO actualizar(String clave, String valor);

    /**
     * Invalida la entrada en caché para una clave específica.
     * <p>
     * Útil si se modifica la base de datos por fuera de este servicio
     * (ej. migración manual, script). La próxima vez que se lea la clave
     * se recargará desde la base de datos.
     *
     * @param clave la clave de configuración a invalidar
     */
    void invalidarCache(String clave);
}
