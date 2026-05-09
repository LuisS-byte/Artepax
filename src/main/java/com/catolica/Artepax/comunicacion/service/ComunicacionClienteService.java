package com.catolica.Artepax.comunicacion.service;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.pago.entity.Pago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComunicacionClienteService {

    private final EmailService emailService;
    private final ConfiguracionService configuracionService;

    private static final String CONFIG_NEGOCIO_NOMBRE = "NEGOCIO_NOMBRE";
    private static final String CONFIG_NEGOCIO_TELEFONO = "NEGOCIO_TELEFONO";

    /**
     * Envía confirmación de reserva con PDF adjunto al cliente.
     */
    public void enviarConfirmacionReserva(Evento evento, byte[] pdfBytes) {
        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("Evento {}: cliente sin correo, no se envía confirmación", evento.getId());
            return;
        }

        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        String asunto = "Confirmación de reserva - " + negocioNombre;
        String cuerpo = construirCuerpoConfirmacion(evento, negocioNombre);
        String nombreArchivo = "confirmacion_evento_" + evento.getId() + ".pdf";

        emailService.enviarConAdjunto(correo, asunto, cuerpo, pdfBytes, nombreArchivo);
        log.info("Confirmación de reserva enviada a cliente del evento {}", evento.getId());
    }

    /**
     * Envía recordatorio de evento próximo al cliente (sin adjunto).
     */
    public void enviarRecordatorioEvento(Evento evento) {
        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("Evento {}: cliente sin correo, no se envía recordatorio", evento.getId());
            return;
        }

        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        String asunto = "Recordatorio de tu evento - " + negocioNombre;
        String cuerpo = construirCuerpoRecordatorio(evento, negocioNombre);

        emailService.enviarSimple(correo, asunto, cuerpo);
        log.info("Recordatorio enviado a cliente del evento {}", evento.getId());
    }

    /**
     * Envía aviso de saldo pendiente al cliente (sin adjunto).
     */
    public void enviarAvisoSaldoPendiente(Evento evento, BigDecimal saldoPendiente) {
        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("Evento {}: cliente sin correo, no se envía aviso de saldo", evento.getId());
            return;
        }

        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        String asunto = "Saldo pendiente - " + negocioNombre;
        String cuerpo = construirCuerpoSaldoPendiente(evento, saldoPendiente, negocioNombre);

        emailService.enviarSimple(correo, asunto, cuerpo);
        log.info("Aviso de saldo pendiente enviado a cliente del evento {}", evento.getId());
    }

    /**
     * Envía comprobante de pago al cliente (sin adjunto).
     */
    public void enviarComprobantePago(Evento evento, Pago pago) {
        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("Evento {}: cliente sin correo, no se envía comprobante", evento.getId());
            return;
        }

        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        String asunto = "Comprobante de pago - " + negocioNombre;
        String cuerpo = construirCuerpoComprobantePago(evento, pago, negocioNombre);

        emailService.enviarSimple(correo, asunto, cuerpo);
        log.info("Comprobante de pago enviado a cliente del evento {}", evento.getId());
    }

    // -------------------------------------------------------------------------
    // TEMPLATES DE EMAIL
    // -------------------------------------------------------------------------

    private String construirCuerpoConfirmacion(Evento evento, String negocioNombre) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

        return "<p>Estimado/a <b>" + evento.getCliente().getNombre() + "</b>,</p>"
                + "<p>Nos complace confirmar la reserva de tu evento con los siguientes detalles:</p>"
                + "<ul>"
                + "<li><b>Tipo:</b> " + safe(evento.getTipoEvento() != null ? evento.getTipoEvento().getTipoNombre() : "") + "</li>"
                + "<li><b>Fecha:</b> " + safe(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : "") + "</li>"
                + "<li><b>Horario:</b> " + safe(formatHora(evento.getHoraInicio(), horaFmt)) + " - " + safe(formatHora(evento.getHoraFin(), horaFmt)) + "</li>"
                + "<li><b>Ubicación:</b> " + safe(evento.getUbicacion()) + "</li>"
                + "<li><b>Decoración:</b> " + safe(evento.getDecoracion() != null ? evento.getDecoracion().getNombre() : "") + "</li>"
                + "</ul>"
                + "<p>Adjuntamos el documento de confirmación. Para cualquier consulta, comunicate al " + configuracionService.getString(CONFIG_NEGOCIO_TELEFONO) + ".</p>"
                + "<p>Gracias por confiar en <b>" + negocioNombre + "</b>.</p>";
    }

    private String construirCuerpoRecordatorio(Evento evento, String negocioNombre) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

        return "<p>Estimado/a <b>" + evento.getCliente().getNombre() + "</b>,</p>"
                + "<p>Te recordamos que tu evento se acerca:</p>"
                + "<ul>"
                + "<li><b>Fecha:</b> " + safe(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : "") + "</li>"
                + "<li><b>Horario:</b> " + safe(formatHora(evento.getHoraInicio(), horaFmt)) + " - " + safe(formatHora(evento.getHoraFin(), horaFmt)) + "</li>"
                + "<li><b>Ubicación:</b> " + safe(evento.getUbicacion()) + "</li>"
                + "</ul>"
                + "<p>Si tenés alguna consulta, no dudes en contactarnos al " + configuracionService.getString(CONFIG_NEGOCIO_TELEFONO) + ".</p>"
                + "<p><b>" + negocioNombre + "</b></p>";
    }

    private String construirCuerpoSaldoPendiente(Evento evento, BigDecimal saldo, String negocioNombre) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return "<p>Estimado/a <b>" + evento.getCliente().getNombre() + "</b>,</p>"
                + "<p>Te informamos que tu evento del <b>" + safe(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : "") + "</b>"
                + " tiene un saldo pendiente de <b>$" + saldo + "</b>.</p>"
                + "<p>Te agradecemos regularizar el pago antes de la fecha del evento. "
                + "Podés comunicarte al " + configuracionService.getString(CONFIG_NEGOCIO_TELEFONO) + " para coordinar.</p>"
                + "<p><b>" + negocioNombre + "</b></p>";
    }

    private String construirCuerpoComprobantePago(Evento evento, Pago pago, String negocioNombre) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return "<p>Estimado/a <b>" + evento.getCliente().getNombre() + "</b>,</p>"
                + "<p>Tu pago fue registrado exitosamente:</p>"
                + "<ul>"
                + "<li><b>Monto:</b> $" + pago.getMonto() + "</li>"
                + "<li><b>Fecha:</b> " + safe(pago.getFecha() != null ? pago.getFecha().format(fechaFmt) : "") + "</li>"
                + "<li><b>Evento:</b> #" + evento.getId() + "</li>"
                + "</ul>"
                + "<p>Gracias por tu pago. <b>" + negocioNombre + "</b></p>";
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String formatHora(java.time.LocalTime hora, DateTimeFormatter fmt) {
        return hora != null ? hora.format(fmt) : "";
    }
}
