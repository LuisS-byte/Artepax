package com.catolica.Artepax.common.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ErrorResponse(int codigo, String mensaje, String timestamp) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ErrorResponse of(int codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, LocalDateTime.now().format(FMT));
    }
}
