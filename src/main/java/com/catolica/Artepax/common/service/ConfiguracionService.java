package com.catolica.Artepax.common.service;



import java.math.BigDecimal;

public interface  ConfiguracionService {

    String getString(String clave);

    Integer getInt(String clave);

    BigDecimal getDecimal(String clave);

    Boolean getBoolean(String clave);
}
