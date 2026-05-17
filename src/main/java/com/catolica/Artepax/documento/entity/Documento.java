package com.catolica.Artepax.documento.entity;

import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a generated document associated with an event.
 * <p>
 * Documents follow a delivery state flow: {@link DocumentoEstadoEnvio#PENDIENTE} →
 * {@link DocumentoEstadoEnvio#ENVIADO} → {@link DocumentoEstadoEnvio#FALLIDO} →
 * {@link DocumentoEstadoEnvio#REENVIADO}. The {@code archivoUrl} field stores the
 * download path to the generated PDF, and {@code docuIntentosEnvio} tracks retry
 * attempts for failed deliveries. A unique constraint prevents duplicate documents
 * of the same type for the same event.
 * </p>
 */
@Entity
@Table(name = "documento",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_documento_evento_tipo",
               columnNames = {"id_evento", "docu_tipo"}))
@Getter
@Setter
@NoArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plantilla")
    private PlantillaDocumento plantilla;

    @Enumerated(EnumType.STRING)
    @Column(name = "docu_tipo", nullable = false, length = 50)
    private DocumentoTipo docuTipo;

    @CreationTimestamp
    @Column(name = "docu_fechageneracion", nullable = false, updatable = false)
    private LocalDateTime docuFechaGeneracion;

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "docu_estado_envio", nullable = false, length = 15)
    private DocumentoEstadoEnvio docuEstadoEnvio = DocumentoEstadoEnvio.PENDIENTE;

    @Column(name = "docu_fecha_envio")
    private LocalDateTime docuFechaEnvio;

    @Column(name = "docu_intentos_envio", nullable = false)
    private Integer docuIntentosEnvio = 0;

}
