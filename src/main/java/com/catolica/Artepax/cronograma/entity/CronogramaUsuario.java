package com.catolica.Artepax.cronograma.entity;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cronograma_usuario")
public class CronogramaUsuario {

    @EmbeddedId
    private CronogramaUsuarioId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCronograma")
    @JoinColumn(name = "id_cronograma")
    private Cronograma cronograma;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "pago_asignado", nullable = false, precision = 10, scale = 2)
    private BigDecimal pagoAsignado = BigDecimal.ZERO;

    @Column(name = "pago_tipo", nullable = false, length = 10)
    private String pagoTipo = "FIJO";

    public CronogramaUsuario() {}

    public CronogramaUsuarioId getId() { return id; }
    public void setId(CronogramaUsuarioId id) { this.id = id; }

    public Cronograma getCronograma() { return cronograma; }
    public void setCronograma(Cronograma cronograma) { this.cronograma = cronograma; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public BigDecimal getPagoAsignado() { return pagoAsignado; }
    public void setPagoAsignado(BigDecimal pagoAsignado) { this.pagoAsignado = pagoAsignado; }

    public String getPagoTipo() { return pagoTipo; }
    public void setPagoTipo(String pagoTipo) { this.pagoTipo = pagoTipo; }
}
