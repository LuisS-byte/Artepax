package com.catolica.Artepax.common.entity;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Registro de auditoría para acciones relevantes del sistema.
 * Preserva trazabilidad con snapshots JSONB de valores anteriores y nuevos.
 */
@Entity
@Table(name = "auditoria")
@Getter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    @Setter
    private Usuario usuario;

    @Column(name = "accion", nullable = false, length = 20)
    @Setter
    private String accion;

    @Column(name = "tabla_afectada", nullable = false, length = 50)
    @Setter
    private String tablaAfectada;

    @Column(name = "id_registro")
    @Setter
    private Long idRegistro;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_anterior")
    @Setter
    private String valorAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_nuevo")
    @Setter
    private String valorNuevo;

    @Column(name = "descripcion", length = 255)
    @Setter
    private String descripcion;

    @Column(name = "fecha", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime fecha;

    public LocalDateTime getFecha() { return fecha; }
}
