package com.catolica.Artepax.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parámetros globales editables por administración.
 * Soporta tipos heterogéneos (INT, DECIMAL, BOOLEAN, STRING, JSON).
 */
@Entity
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
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
}
