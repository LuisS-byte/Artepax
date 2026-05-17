package com.catolica.Artepax.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ArtepaxException.class)
    public ResponseEntity<ErrorResponse> handleArtepaxException(ArtepaxException ex) {
        ErrorResponse error = ErrorResponse.of(ex.getStatus().value(), ex.getMessage(), ex.getErrorCode());
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class) // P14
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleIllegalState(IllegalStateException ex) {
        return ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Datos de entrada inválidos");
        return ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), mensaje);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        ErrorResponse error = ErrorResponse.of(ex.getStatusCode().value(), ex.getReason());
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return ErrorResponse.of(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMessage());
        String msg = ex.getMessage();
        if (msg == null) {
            return ErrorResponse.of(HttpStatus.CONFLICT.value(), "Error de integridad de datos");
        }

        if (msg.contains("uk_documento_evento_tipo")) {
            return ErrorResponse.of(HttpStatus.CONFLICT.value(),
                    "Ya existe un documento de este tipo para el evento especificado");
        }
        if (msg.contains("idx_cliente_correo_unique")) {
            return ErrorResponse.of(HttpStatus.CONFLICT.value(),
                    "Ya existe un cliente con ese correo electrónico");
        }
        if (msg.contains("idx_usuario_correo_lower")) {
            return ErrorResponse.of(HttpStatus.CONFLICT.value(),
                    "Ya existe un usuario con ese correo electrónico");
        }
        if (msg.contains("tipo_evento_tipo_nombre_key")) {
            return ErrorResponse.of(HttpStatus.CONFLICT.value(),
                    "Ya existe un tipo de evento con ese nombre");
        }
        if (msg.contains("metodo_pago_metodo_nombre_key")) {
            return ErrorResponse.of(HttpStatus.CONFLICT.value(),
                    "Ya existe un método de pago con ese nombre");
        }
        if (msg.contains("producto_produ_nombre_key")) {
            return ErrorResponse.of(HttpStatus.CONFLICT.value(),
                    "Ya existe un producto con ese nombre");
        }
        return ErrorResponse.of(HttpStatus.CONFLICT.value(), "Error de integridad de datos");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        log.error("Error interno no manejado: {}", ex.getMessage(), ex);
        return ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor");
    }
}
