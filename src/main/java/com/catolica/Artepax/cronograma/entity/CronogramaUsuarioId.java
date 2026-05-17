package com.catolica.Artepax.cronograma.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave compuesta para la tabla puente {@code cronograma_usuario}.
 * Combina el ID de la actividad y el ID del usuario.
 */
@Embeddable
public class CronogramaUsuarioId implements Serializable {

    /** Identificador de la actividad del cronograma. */
    @Column(name = "id_cronograma")
    private Long idCronograma;

    /** Identificador del usuario. */
    @Column(name = "id_usuario")
    private Long idUsuario;

    public CronogramaUsuarioId() {}

    /**
     * Crea una clave compuesta con los identificadores dados.
     *
     * @param idCronograma id de la actividad del cronograma
     * @param idUsuario    id del usuario
     */
    public CronogramaUsuarioId(Long idCronograma, Long idUsuario) {
        this.idCronograma = idCronograma;
        this.idUsuario = idUsuario;
    }

    /**
     * Retorna el identificador de la actividad del cronograma.
     *
     * @return id del cronograma
     */
    public Long getIdCronograma() { return idCronograma; }

    /**
     * Asigna el identificador de la actividad del cronograma.
     *
     * @param idCronograma id del cronograma
     */
    public void setIdCronograma(Long idCronograma) { this.idCronograma = idCronograma; }

    /**
     * Retorna el identificador del usuario.
     *
     * @return id del usuario
     */
    public Long getIdUsuario() { return idUsuario; }

    /**
     * Asigna el identificador del usuario.
     *
     * @param idUsuario id del usuario
     */
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CronogramaUsuarioId that)) return false;
        return Objects.equals(idCronograma, that.idCronograma) &&
               Objects.equals(idUsuario, that.idUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCronograma, idUsuario);
    }
}
