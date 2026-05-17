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

/**
 * Entidad central del dominio de eventos.
 * <p>
 * Representa un evento de decoración con su ciclo de vida completo gobernado
 * por una máquina de estados: COTIZACION → PROGRAMADO → CONFIRMADO → EN_MONTAJE
 * → (FINALIZADO | PENDIENTE_COBRO → FINALIZADO). La cancelación puede ocurrir
 * directa (COTIZACION) o con tiempo de gracia reversible (CANCELACION_PENDIENTE).
 * <p>
 * Los precios se componen de un {@code precioBase} (costo de la decoración
 * seleccionada) y un {@code precioAdicional} (cargos por extras). El campo
 * {@code precioTotal} es una columna generada por la base de datos que suma
 * ambos.
 */
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

    /** Cliente contratante del evento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    /** Decoración seleccionada para el evento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_decoracion", nullable = false)
    private Decoracion decoracion;

    /** Tipo de evento (cumpleaños, boda, etc.) del catálogo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evento", nullable = false)
    private TipoEvento tipoEvento;

    /** Estado actual dentro del ciclo de vida del evento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estadoevento", nullable = false)
    private EstadoEvento estadoEvento;

    /**
     * Estado previo a la cancelación, utilizado para revertir desde
     * CANCELACION_PENDIENTE al estado original.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_anterior")
    private EstadoEvento estadoAnterior;

    /** Fecha en que se realizará el evento. */
    @Column(name = "event_fechaevento", nullable = false)
    private LocalDate fecha;

    /** Hora de inicio del evento. */
    @Column(name = "event_horainicio")
    private LocalTime horaInicio;

    /** Hora de finalización del evento. */
    @Column(name = "event_horafin")
    private LocalTime horaFin;

    /** Ubicación donde se realizará el evento. */
    @Column(name = "event_ubicacion", length = 255)
    private String ubicacion;

    /** Precio base del evento, corresponde al precio de la decoración seleccionada. */
    @Column(name = "event_precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    /** Cargo adicional sobre el precio base por productos extras. */
    @Column(name = "event_precio_adicional", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioAdicional;

    /**
     * Precio total del evento (precioBase + precioAdicional).
     * Columna computada por la base de datos ({@code insertable = false, updatable = false}).
     */
    @Column(name = "event_precio_total", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal precioTotal;

    /** Justificación del precio adicional (obligatoria si precioAdicional > 0). */
    @Column(name = "event_nota_precio", length = 255)
    private String notaPrecio;

    /** Nota interna o comentario sobre el evento. */
    @Column(name = "event_nota", length = 255)
    private String nota;

    /** Motivo de la cancelación, requerido cuando se cancela el evento. */
    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    /** Indica si el cliente fue notificado sobre cambios de estado. */
    @Column(name = "event_cliente_notificado", nullable = false)
    private Boolean clienteNotificado;

    /** Fecha y hora en que el evento fue confirmado (transición a CONFIRMADO). */
    @Column(name = "event_fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    /** Fecha y hora en que se inició o confirmó la cancelación del evento. */
    @Column(name = "event_fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    /** Fecha de vencimiento de la cotización. Pasada esta fecha, la cotización expira. */
    @Column(name = "event_fecha_expiracion")
    private LocalDate fechaExpiracion;

    /** Fecha y hora de creación del registro, asignada automáticamente por Hibernate. */
    @Column(name = "event_creacion", updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaCreacion;
}
