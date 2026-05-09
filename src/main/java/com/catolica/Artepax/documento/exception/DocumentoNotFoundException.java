package com.catolica.Artepax.documento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class DocumentoNotFoundException extends ArtepaxException {

    public DocumentoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Documento no encontrado: " + id);
    }
}
