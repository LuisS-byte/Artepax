package com.catolica.Artepax.evento.model;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_decoracion", nullable = false)
    private Decoracion decoracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evento", nullable = false)
    private TipoEvento tipoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estadoevento", nullable = false)
    private EstadoEvento estadoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_anterior")
    private EstadoEvento estadoAnterior;

    @Column(name = "event_fechaevento", nullable = false)
    private LocalDate fecha;

    @Column(name = "event_horainicio")
    private LocalTime horaInicio;

    @Column(name = "event_horafin")
    private LocalTime horaFin;

    @Column(name = "event_ubicacion", length = 255)
    private String ubicacion;

    @Column(name = "event_precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "event_precio_adicional", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioAdicional;

    @Column(name = "event_precio_total", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal precioTotal;

    @Column(name = "event_nota_precio", length = 255)
    private String notaPrecio;

    @Column(name = "event_nota", length = 255)
    private String nota;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Column(name = "event_cliente_notificado", nullable = false)
    private Boolean clienteNotificado;

    @Column(name = "event_fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "event_fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "event_fecha_expiracion")
    private LocalDate fechaExpiracion;

    @Column(name = "event_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaCreacion;
}
