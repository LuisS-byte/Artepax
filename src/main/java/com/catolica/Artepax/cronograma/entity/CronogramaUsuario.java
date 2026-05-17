package com.catolica.Artepax.cronograma.entity;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Asignación de un usuario a una actividad del cronograma con información de pago.
 * El pago puede ser FIJO (monto por actividad) o POR_HORA (basado en horas).
 */
@Entity
@Table(name = "cronograma_usuario")
public class CronogramaUsuario {

    @EmbeddedId
    private CronogramaUsuarioId id;

    /** Actividad del cronograma asignada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCronograma")
    @JoinColumn(name = "id_cronograma")
    private Cronograma cronograma;

    /** Usuario asignado a la actividad. */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    /** Monto o tarifa pactada por la asignación. */
    @Column(name = "pago_asignado", nullable = false, precision = 10, scale = 2)
    private BigDecimal pagoAsignado = BigDecimal.ZERO;

    /** Tipo de pago: FIJO o POR_HORA. */
    @Column(name = "pago_tipo", nullable = false, length = 10)
    private String pagoTipo = CronogramaPagoTipo.FIJO.getValor();

    public CronogramaUsuario() {}

    /**
     * Retorna la clave compuesta de la asignación.
     *
     * @return id compuesto (cronogramaId, usuarioId)
     */
    public CronogramaUsuarioId getId() { return id; }

    /**
     * Asigna la clave compuesta de la asignación.
     *
     * @param id id compuesto
     */
    public void setId(CronogramaUsuarioId id) { this.id = id; }

    /**
     * Retorna la actividad del cronograma asignada.
     *
     * @return actividad asociada
     */
    public Cronograma getCronograma() { return cronograma; }

    /**
     * Asigna la actividad del cronograma.
     *
     * @param cronograma actividad asociada
     */
    public void setCronograma(Cronograma cronograma) { this.cronograma = cronograma; }

    /**
     * Retorna el usuario asignado a la actividad.
     *
     * @return usuario asignado
     */
    public Usuario getUsuario() { return usuario; }

    /**
     * Asigna el usuario a la actividad.
     *
     * @param usuario usuario a asignar
     */
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    /**
     * Retorna el monto o tarifa pactada.
     *
     * @return pago asignado
     */
    public BigDecimal getPagoAsignado() { return pagoAsignado; }

    /**
     * Asigna el monto o tarifa pactada.
     *
     * @param pagoAsignado pago asignado
     */
    public void setPagoAsignado(BigDecimal pagoAsignado) { this.pagoAsignado = pagoAsignado; }

    /**
     * Retorna el tipo de pago.
     *
     * @return "FIJO" o "POR_HORA"
     */
    public String getPagoTipo() { return pagoTipo; }

    /**
     * Asigna el tipo de pago.
     *
     * @param pagoTipo "FIJO" o "POR_HORA"
     */
    public void setPagoTipo(String pagoTipo) { this.pagoTipo = pagoTipo; }
}
