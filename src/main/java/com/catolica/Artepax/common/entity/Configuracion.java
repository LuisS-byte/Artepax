package com.catolica.Artepax.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracion")
public class Configuracion {

    @Id
    @Column(name = "config_clave", length = 100)
    private String configClave;

    @Column(name = "config_valor", nullable = false, columnDefinition = "TEXT")
    private String configValor;

    @Column(name = "config_tipo", nullable = false, length = 10)
    private String configTipo = "STRING";

    @Column(name = "config_descripcion", length = 255)
    private String configDescripcion;

    public Configuracion() {}

    public String getConfigClave() { return configClave; }
    public void setConfigClave(String configClave) { this.configClave = configClave; }

    public String getConfigValor() { return configValor; }
    public void setConfigValor(String configValor) { this.configValor = configValor; }

    public String getConfigTipo() { return configTipo; }
    public void setConfigTipo(String configTipo) { this.configTipo = configTipo; }

    public String getConfigDescripcion() { return configDescripcion; }
    public void setConfigDescripcion(String configDescripcion) { this.configDescripcion = configDescripcion; }
}
