package com.catolica.Artepax.documento.exception;

import com.catolica.Artepax.common.exception.ArtepaxException;
import org.springframework.http.HttpStatus;

public class PlantillaDocumentoNotFoundException extends ArtepaxException {

    public PlantillaDocumentoNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Plantilla no encontrada: " + id);
    }
}
