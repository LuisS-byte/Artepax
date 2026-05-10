package com.catolica.Artepax.common.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ErrorResponse(int codigo, String mensaje, String errorCode, String timestamp) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ErrorResponse of(int codigo, String mensaje) {
        return of(codigo, mensaje, null);
    }

    public static ErrorResponse of(int codigo, String mensaje, String errorCode) {
        return new ErrorResponse(codigo, mensaje, errorCode, LocalDateTime.now().format(FMT));
    }
}
