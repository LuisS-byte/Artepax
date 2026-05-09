package com.catolica.Artepax.documento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class DocumentoOperacionNoPermitidaException extends ArtepaxException {

    public DocumentoOperacionNoPermitidaException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
