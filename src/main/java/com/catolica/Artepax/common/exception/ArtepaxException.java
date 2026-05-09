package com.catolica.Artepax.common.exception;

import org.springframework.http.HttpStatus;

public abstract class ArtepaxException extends RuntimeException {

    private final HttpStatus status;

    protected ArtepaxException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected ArtepaxException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
