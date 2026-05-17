package com.catolica.Artepax.common.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO inmutable para respuestas de error HTTP uniformes.
 * <p>
 * Es el formato con el que {@link GlobalExceptionHandler} serializa
 * toda excepción hacia el cliente, incluyendo código HTTP, mensaje,
 * código de error opcional y timestamp ISO.
 */
public record ErrorResponse(int codigo, String mensaje, String errorCode, String timestamp) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Crea una respuesta de error sin código de error adicional.
     *
     * @param codigo  el código HTTP numérico
     * @param mensaje descripción legible del error
     * @return una nueva {@link ErrorResponse}
     */
    public static ErrorResponse of(int codigo, String mensaje) {
        return of(codigo, mensaje, null);
    }

    /**
     * Crea una respuesta de error con código de error opcional.
     *
     * @param codigo    el código HTTP numérico
     * @param mensaje   descripción legible del error
     * @param errorCode código alfanumérico para identificar el tipo de error
     * @return una nueva {@link ErrorResponse}
     */
    public static ErrorResponse of(int codigo, String mensaje, String errorCode) {
        return new ErrorResponse(codigo, mensaje, errorCode, LocalDateTime.now().format(FMT));
    }
}
