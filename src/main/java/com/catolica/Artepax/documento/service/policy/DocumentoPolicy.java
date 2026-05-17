package com.catolica.Artepax.documento.service.policy;

import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.DocumentoTipo;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import com.catolica.Artepax.documento.exception.DocumentoOperacionNoPermitidaException;
import com.catolica.Artepax.evento.model.EstadoEventoNames;
import com.catolica.Artepax.evento.model.Evento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class DocumentoPolicy {

    private static final int MAX_INTENTOS_ENVIO = 3;

    private static final Set<String> ESTADOS_CONFIRMACION_RESERVA = Set.of(
            EstadoEventoNames.CONFIRMADO,
            EstadoEventoNames.EN_MONTAJE,
            EstadoEventoNames.FINALIZADO
    );

    private static final Set<String> ESTADOS_COTIZACION_EVENTO = Set.of(
            EstadoEventoNames.COTIZACION
    );

    private static final Set<String> ESTADOS_AVISO_SALDO_PENDIENTE = Set.of(
            EstadoEventoNames.PROGRAMADO,
            EstadoEventoNames.CONFIRMADO,
            EstadoEventoNames.EN_MONTAJE,
            EstadoEventoNames.PENDIENTE_COBRO,
            EstadoEventoNames.FINALIZADO
    );

    public void validarGeneracionPermitida(Evento evento, DocumentoTipo tipoDocumento) {
        String estadoEvento = evento.getEstadoEvento().getNombre();
        boolean permitida = switch (tipoDocumento) {
            case CONFIRMACION_RESERVA -> ESTADOS_CONFIRMACION_RESERVA.contains(estadoEvento);
            case COTIZACION_EVENTO -> ESTADOS_COTIZACION_EVENTO.contains(estadoEvento);
            case AVISO_SALDO_PENDIENTE -> ESTADOS_AVISO_SALDO_PENDIENTE.contains(estadoEvento);
            case SALDO_PENDIENTE -> throw new DocumentoOperacionNoPermitidaException(
                    "El tipo SALDO_PENDIENTE no está habilitado directamente. Use AVISO_SALDO_PENDIENTE.");
            default -> true;
        };

        if (!permitida) {
            throw new DocumentoOperacionNoPermitidaException(
                    "No se puede generar " + tipoDocumento.name() + " para un evento en estado " + estadoEvento);
        }
    }

    public void validarSinDocumentoDuplicado(Long eventoId, DocumentoTipo tipoDocumento, boolean yaExiste) {
        if (yaExiste) {
            throw new DocumentoOperacionNoPermitidaException(
                    "Ya existe un documento de tipo " + tipoDocumento.name() + " para este evento.");
        }
    }

    public void validarSaldoPendiente(BigDecimal saldoPendiente) {
        if (saldoPendiente == null || saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DocumentoOperacionNoPermitidaException(
                    "No se puede generar AVISO_SALDO_PENDIENTE: el saldo pendiente es cero o menor.");
        }
    }

    public void validarReenvioPermitido(Documento documento) {
        validarIntentosReenvio(documento);
        validarCorreoCliente(documento.getEvento());
    }

    public void validarPlantillaDisponible(PlantillaDocumento plantilla) {
        if (plantilla == null || !Boolean.TRUE.equals(plantilla.getActivo())) {
            throw new DocumentoOperacionNoPermitidaException("No hay una plantilla activa disponible para este documento.");
        }
    }

    public void validarCorreoCliente(Evento evento) {
        String correo = evento.getCliente() != null ? evento.getCliente().getCorreo() : null;
        if (correo == null || correo.isBlank()) {
            throw new DocumentoOperacionNoPermitidaException(
                    "No se puede reenviar: el cliente no tiene correo registrado.");
        }
    }

    public void validarIntentosReenvio(Documento documento) {
        if (documento.getDocuIntentosEnvio() >= MAX_INTENTOS_ENVIO) {
            throw new DocumentoOperacionNoPermitidaException(
                    "Se alcanzó el límite de " + MAX_INTENTOS_ENVIO + " intentos de envío.");
        }
    }
}
