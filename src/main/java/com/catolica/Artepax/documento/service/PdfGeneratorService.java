package com.catolica.Artepax.documento.service;

import com.catolica.Artepax.documento.exception.PdfGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.openpdf.pdf.ITextRenderer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Service for converting HTML content into PDF documents.
 * <p>
 * Uses an in-memory renderer to transform HTML strings (with placeholders already
 * substituted) into PDF byte arrays suitable for email attachment or disk storage.
 * </p>
 */
@Service
@Slf4j
public class PdfGeneratorService {

    /**
     * Renders an HTML string into a PDF document in memory.
     *
     * @param html the HTML content with placeholders already replaced
     * @return the PDF as a byte array
     * @throws PdfGenerationException if rendering or PDF creation fails
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
