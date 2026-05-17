package com.catolica.Artepax.comunicacion.service;

import com.catolica.Artepax.comunicacion.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails via the configured {@link JavaMailSender}.
 * <p>
 * Provides two delivery modes: simple HTML text and HTML text with a PDF
 * attachment. Both methods validate input parameters and wrap
 * {@link jakarta.mail.MessagingException} into {@link RuntimeException}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    /**
     * Sends an email with a PDF attachment to the given recipient.
     *
     * @param destinatario recipient email address (must not be blank)
     * @param asunto       email subject (must not be blank)
     * @param cuerpo       email body in HTML format (must not be blank)
     * @param pdfBytes     byte array containing the PDF content (must not be empty)
     * @param nombreArchivo  file name for the attached PDF (must not be blank)
     * @throws IllegalArgumentException if any parameter is null or blank
     * @throws RuntimeException         if the email cannot be sent
     */
    public void enviarConAdjunto(String destinatario, String asunto,
                                 String cuerpo, byte[] pdfBytes, String nombreArchivo) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El destinatario no puede estar vacío.");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("El asunto no puede estar vacío.");
        }
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException("El cuerpo no puede estar vacío.");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("El archivo PDF adjunto no puede estar vacío.");
        }
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo adjunto no puede estar vacío.");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpo, true);

            helper.addAttachment(nombreArchivo,
                    new org.springframework.core.io.ByteArrayResource(pdfBytes),
                    "application/pdf");

            mailSender.send(mensaje);
            log.info("Email enviado correctamente a: {}", destinatario);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Error enviando email a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar el email: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a plain HTML email without attachments.
     *
     * @param destinatario recipient email address (must not be blank)
     * @param asunto       email subject (must not be blank)
     * @param cuerpo       email body in HTML format (must not be blank)
     * @throws IllegalArgumentException if any parameter is null or blank
     * @throws RuntimeException         if the email cannot be sent
     */
    public void enviarSimple(String destinatario, String asunto, String cuerpo) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException("El destinatario no puede estar vacío.");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("El asunto no puede estar vacío.");
        }
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException("El cuerpo no puede estar vacío.");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpo, true);

            mailSender.send(mensaje);
            log.info("Email simple enviado correctamente a: {}", destinatario);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Error enviando email simple a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar el email: " + e.getMessage(), e);
        }
    }
}
