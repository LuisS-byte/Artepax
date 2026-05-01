package com.catolica.Artepax.common.dto;

public class ConfiguracionResponseDTO {
    private String configClave;
    private String configValor;
    private String configTipo;
    private String configDescripcion;

    public String getConfigClave() { return configClave; }
    public void setConfigClave(String configClave) { this.configClave = configClave; }

    public String getConfigValor() { return configValor; }
    public void setConfigValor(String configValor) { this.configValor = configValor; }

    public String getConfigTipo() { return configTipo; }
    public void setConfigTipo(String configTipo) { this.configTipo = configTipo; }

    public String getConfigDescripcion() { return configDescripcion; }
    public void setConfigDescripcion(String configDescripcion) { this.configDescripcion = configDescripcion; }
}
