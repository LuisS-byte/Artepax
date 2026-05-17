package com.catolica.Artepax.documento.service;

import com.catolica.Artepax.documento.exception.PdfGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.openpdf.pdf.ITextRenderer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class PdfGeneratorService {
    /**
     * Convierte un HTML (con placeholders ya reemplazados) a PDF en memoria.
     * Retorna el PDF como array de bytes listo para enviar por email o guardar.
     */
    public byte[] generarDesdeHtml(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);
            log.info("PDF generado correctamente ({} bytes)", outputStream.size());
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new PdfGenerationException("Error al generar el PDF: " + e.getMessage(), e);
        }
    }

}
