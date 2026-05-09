package com.catolica.Artepax.comunicacion.service;

import com.catolica.Artepax.comunicacion.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void enviarConAdjunto(String destinatario, String asunto,
                                 String cuerpo, byte[] pdfBytes, String nombreArchivo) {
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

    public void enviarSimple(String destinatario, String asunto, String cuerpo) {
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
