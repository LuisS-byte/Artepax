package com.catolica.Artepax.comunicacion.service;

import com.catolica.Artepax.comunicacion.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailProperties mailProperties;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService service;

    @BeforeEach
    void setUp() throws MessagingException, java.io.UnsupportedEncodingException {
        service = new EmailService(mailSender, mailProperties);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        lenient().when(mailProperties.getFrom()).thenReturn("noreply@artepax.com");
        lenient().when(mailProperties.getFromName()).thenReturn("Artepax");
    }

    // --- enviarSimple validation ---

    @Test
    void enviarSimple_destinatarioNull_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarSimple(null, "asunto", "cuerpo"));
    }

    @Test
    void enviarSimple_destinatarioBlank_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarSimple("   ", "asunto", "cuerpo"));
    }

    @Test
    void enviarSimple_asuntoNull_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarSimple("test@test.com", null, "cuerpo"));
    }

    @Test
    void enviarSimple_asuntoBlank_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarSimple("test@test.com", "  ", "cuerpo"));
    }

    @Test
    void enviarSimple_cuerpoNull_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarSimple("test@test.com", "asunto", null));
    }

    @Test
    void enviarSimple_cuerpoBlank_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarSimple("test@test.com", "asunto", ""));
    }

    // --- enviarConAdjunto validation ---

    @Test
    void enviarConAdjunto_destinatarioNull_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarConAdjunto(null, "asunto", "cuerpo", new byte[]{1}, "file.pdf"));
    }

    @Test
    void enviarConAdjunto_asuntoBlank_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarConAdjunto("test@test.com", "", "cuerpo", new byte[]{1}, "file.pdf"));
    }

    @Test
    void enviarConAdjunto_cuerpoBlank_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarConAdjunto("test@test.com", "asunto", "   ", new byte[]{1}, "file.pdf"));
    }

    @Test
    void enviarConAdjunto_pdfBytesNull_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarConAdjunto("test@test.com", "asunto", "cuerpo", null, "file.pdf"));
    }

    @Test
    void enviarConAdjunto_pdfBytesEmpty_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarConAdjunto("test@test.com", "asunto", "cuerpo", new byte[0], "file.pdf"));
    }

    @Test
    void enviarConAdjunto_nombreArchivoNull_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarConAdjunto("test@test.com", "asunto", "cuerpo", new byte[]{1}, null));
    }

    @Test
    void enviarConAdjunto_nombreArchivoBlank_lanzaException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviarConAdjunto("test@test.com", "asunto", "cuerpo", new byte[]{1}, "  "));
    }

    // --- happy path (mocked) ---

    @Test
    void enviarSimple_valido_enviaSinError() {
        assertDoesNotThrow(() -> service.enviarSimple("test@test.com", "asunto", "<p>cuerpo</p>"));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void enviarConAdjunto_valido_enviaSinError() {
        assertDoesNotThrow(() -> service.enviarConAdjunto("test@test.com", "asunto", "<p>cuerpo</p>", new byte[]{1, 2}, "doc.pdf"));
        verify(mailSender).send(mimeMessage);
    }
}
