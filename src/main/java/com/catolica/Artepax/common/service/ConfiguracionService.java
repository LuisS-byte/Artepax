package com.catolica.Artepax.common.service;



import com.catolica.Artepax.common.dto.ConfiguracionResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public interface  ConfiguracionService {

    String getString(String clave);
    Integer getInt(String clave);
    BigDecimal getDecimal(String clave);
    Boolean getBoolean(String clave);

    List<ConfiguracionResponseDTO> listar();                          // P6
    ConfiguracionResponseDTO obtener(String clave);                   // P6
    ConfiguracionResponseDTO actualizar(String clave, String valor);  // P6
    void invalidarCache(String clave);
}
