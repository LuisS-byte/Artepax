package com.catolica.Artepax.documento.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    /**
     * Envía un email con el PDF adjunto al destinatario indicado.
     *
     * @param destinatario correo del cliente
     * @param asunto       asunto del email
     * @param cuerpo       texto HTML del cuerpo del email
     * @param pdfBytes     PDF generado como bytes
     * @param nombreArchivo nombre del archivo adjunto (ej: "confirmacion_evento_1.pdf")
     */
    public void enviarConAdjunto(String destinatario, String asunto,
                                 String cuerpo, byte[] pdfBytes, String nombreArchivo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpo, true); // true = es HTML

            // Adjuntar el PDF
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

}
