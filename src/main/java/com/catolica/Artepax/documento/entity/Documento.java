package com.catolica.Artepax.documento.entity;

import com.catolica.Artepax.evento.model.Evento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento")
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

    @Column(name = "docu_tipo", nullable = false, length = 50)
    private String docuTipo;

    @CreationTimestamp
    @Column(name = "docu_fechageneracion", nullable = false, updatable = false)
    private LocalDateTime docuFechaGeneracion;

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    // CHECK en BD: PENDIENTE / ENVIADO / FALLIDO / REENVIADO
    @Column(name = "docu_estado_envio", nullable = false, length = 15)
    private String docuEstadoEnvio = "PENDIENTE";

    @Column(name = "docu_fecha_envio")
    private LocalDateTime docuFechaEnvio;

    @Column(name = "docu_intentos_envio", nullable = false)
    private Integer docuIntentosEnvio = 0;

}
