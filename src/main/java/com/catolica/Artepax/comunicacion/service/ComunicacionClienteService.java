package com.catolica.Artepax.comunicacion.service;

import com.catolica.Artepax.common.service.ConfiguracionService;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.pago.entity.Pago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Service for customer-facing email communication.
 * <p>
 * Builds HTML email templates for booking confirmations, event reminders,
 * pending-balance notices, and payment receipts. All template content is
 * HTML-escaped before sending. Delegates actual delivery to {@link EmailService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComunicacionClienteService {

    private final EmailService emailService;
    private final ConfiguracionService configuracionService;

    private static final String CONFIG_NEGOCIO_NOMBRE = "NEGOCIO_NOMBRE";
    private static final String CONFIG_NEGOCIO_TELEFONO = "NEGOCIO_TELEFONO";

    /**
     * Sends a booking confirmation email with a PDF attachment to the event client.
     *
     * @param evento   the event for which the confirmation is sent
     * @param pdfBytes byte array containing the confirmation PDF document
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
     * Sends an upcoming-event reminder email to the client (no attachment).
     *
     * @param evento the event to remind about
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
     * Sends a pending-balance notice with a PDF document to the client.
     *
     * @param evento   the event with an outstanding balance
     * @param pdfBytes byte array containing the formal notice PDF
     * @throws IllegalArgumentException if the client has no registered email
     */
    public void enviarAvisoSaldoPendienteConDocumento(Evento evento, byte[] pdfBytes) {
        String correo = evento.getCliente().getCorreo();
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El cliente no tiene correo registrado.");
        }

        String negocioNombre = configuracionService.getString(CONFIG_NEGOCIO_NOMBRE);
        String asunto = "Aviso de saldo pendiente - " + negocioNombre;
        String cuerpo = construirCuerpoAvisoSaldoPendiente(evento, negocioNombre);
        String nombreArchivo = "aviso_saldo_pendiente_" + evento.getId() + ".pdf";

        emailService.enviarConAdjunto(correo, asunto, cuerpo, pdfBytes, nombreArchivo);
        log.info("Aviso de saldo pendiente con documento enviado a cliente del evento {}", evento.getId());
    }

    /**
     * Sends a pending-balance reminder to the client (no attachment).
     *
     * @param evento         the event with an outstanding balance
     * @param saldoPendiente the amount still owed by the client
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
     * Sends a payment receipt to the client (no attachment).
     *
     * @param evento the event associated with the payment
     * @param pago   the confirmed payment to be notified
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

        return "<p>Estimado/a <b>" + escapeHtml(evento.getCliente().getNombre()) + "</b>,</p>"
                + "<p>Nos complace confirmar la reserva de tu evento con los siguientes detalles:</p>"
                + "<ul>"
                + "<li><b>Tipo:</b> " + escapeHtml(evento.getTipoEvento() != null ? evento.getTipoEvento().getTipoNombre() : "") + "</li>"
                + "<li><b>Fecha:</b> " + escapeHtml(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : "") + "</li>"
                + "<li><b>Horario:</b> " + escapeHtml(formatHora(evento.getHoraInicio(), horaFmt)) + " - " + escapeHtml(formatHora(evento.getHoraFin(), horaFmt)) + "</li>"
                + "<li><b>Ubicación:</b> " + escapeHtml(evento.getUbicacion()) + "</li>"
                + "<li><b>Decoración:</b> " + escapeHtml(evento.getDecoracion() != null ? evento.getDecoracion().getNombre() : "") + "</li>"
                + "</ul>"
                + "<p>Adjuntamos el documento de confirmación. Para cualquier consulta, comunicate al " + escapeHtml(configuracionService.getString(CONFIG_NEGOCIO_TELEFONO)) + ".</p>"
                + "<p>Gracias por confiar en <b>" + escapeHtml(negocioNombre) + "</b>.</p>";
    }

    private String construirCuerpoRecordatorio(Evento evento, String negocioNombre) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

        return "<p>Estimado/a <b>" + escapeHtml(evento.getCliente().getNombre()) + "</b>,</p>"
                + "<p>Te recordamos que tu evento se acerca:</p>"
                + "<ul>"
                + "<li><b>Fecha:</b> " + escapeHtml(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : "") + "</li>"
                + "<li><b>Horario:</b> " + escapeHtml(formatHora(evento.getHoraInicio(), horaFmt)) + " - " + escapeHtml(formatHora(evento.getHoraFin(), horaFmt)) + "</li>"
                + "<li><b>Ubicación:</b> " + escapeHtml(evento.getUbicacion()) + "</li>"
                + "</ul>"
                + "<p>Si tenés alguna consulta, no dudes en contactarnos al " + escapeHtml(configuracionService.getString(CONFIG_NEGOCIO_TELEFONO)) + ".</p>"
                + "<p><b>" + escapeHtml(negocioNombre) + "</b></p>";
    }

    private String construirCuerpoAvisoSaldoPendiente(Evento evento, String negocioNombre) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return "<p>Estimado/a <b>" + escapeHtml(evento.getCliente().getNombre()) + "</b>,</p>"
                + "<p>Le informamos que su evento aún registra un saldo pendiente. "
                + "Adjuntamos el documento formal con los detalles.</p>"
                + "<ul>"
                + "<li><b>Tipo de evento:</b> " + escapeHtml(evento.getTipoEvento() != null ? evento.getTipoEvento().getTipoNombre() : "") + "</li>"
                + "<li><b>Fecha:</b> " + escapeHtml(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : "") + "</li>"
                + "<li><b>Ubicación:</b> " + escapeHtml(evento.getUbicacion()) + "</li>"
                + "</ul>"
                + "<p>Si ya realizó el pago, puede ignorar este aviso o comunicarse con nosotros al "
                + escapeHtml(configuracionService.getString(CONFIG_NEGOCIO_TELEFONO)) + " para confirmar la actualización.</p>"
                + "<p><b>" + escapeHtml(negocioNombre) + "</b></p>";
    }

    private String construirCuerpoSaldoPendiente(Evento evento, BigDecimal saldo, String negocioNombre) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return "<p>Estimado/a <b>" + escapeHtml(evento.getCliente().getNombre()) + "</b>,</p>"
                + "<p>Te informamos que tu evento del <b>" + escapeHtml(evento.getFecha() != null ? evento.getFecha().format(fechaFmt) : "") + "</b>"
                + " tiene un saldo pendiente de <b>$" + saldo + "</b>.</p>"
                + "<p>Te agradecemos regularizar el pago antes de la fecha del evento. "
                + "Podés comunicarte al " + escapeHtml(configuracionService.getString(CONFIG_NEGOCIO_TELEFONO)) + " para coordinar.</p>"
                + "<p><b>" + escapeHtml(negocioNombre) + "</b></p>";
    }

    private String construirCuerpoComprobantePago(Evento evento, Pago pago, String negocioNombre) {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return "<p>Estimado/a <b>" + escapeHtml(evento.getCliente().getNombre()) + "</b>,</p>"
                + "<p>Tu pago fue registrado exitosamente:</p>"
                + "<ul>"
                + "<li><b>Monto:</b> $" + pago.getMonto() + "</li>"
                + "<li><b>Fecha:</b> " + escapeHtml(pago.getFecha() != null ? pago.getFecha().format(fechaFmt) : "") + "</li>"
                + "<li><b>Evento:</b> #" + evento.getId() + "</li>"
                + "</ul>"
                + "<p>Gracias por tu pago. <b>" + escapeHtml(negocioNombre) + "</b></p>";
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(value);
    }

    private String formatHora(java.time.LocalTime hora, DateTimeFormatter fmt) {
        return hora != null ? hora.format(fmt) : "";
    }
}
