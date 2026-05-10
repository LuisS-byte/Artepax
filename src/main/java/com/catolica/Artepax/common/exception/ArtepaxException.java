package com.catolica.Artepax.common.exception;

import org.springframework.http.HttpStatus;

public abstract class ArtepaxException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ArtepaxException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    protected ArtepaxException(HttpStatus status, String message, String errorCode) {
        this(status, message, errorCode, null);
    }

    protected ArtepaxException(HttpStatus status, String message, Throwable cause) {
        this(status, message, null, cause);
    }

    protected ArtepaxException(HttpStatus status, String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
