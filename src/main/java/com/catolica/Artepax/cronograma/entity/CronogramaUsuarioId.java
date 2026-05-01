package com.catolica.Artepax.cronograma.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CronogramaUsuarioId implements Serializable {

    @Column(name = "id_cronograma")
    private Long idCronograma;

    @Column(name = "id_usuario")
    private Long idUsuario;

    public CronogramaUsuarioId() {}

    public CronogramaUsuarioId(Long idCronograma, Long idUsuario) {
        this.idCronograma = idCronograma;
        this.idUsuario = idUsuario;
    }

    public Long getIdCronograma() { return idCronograma; }
    public void setIdCronograma(Long idCronograma) { this.idCronograma = idCronograma; }

    public Long getIdUsuario() { return idUsuario; }
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
