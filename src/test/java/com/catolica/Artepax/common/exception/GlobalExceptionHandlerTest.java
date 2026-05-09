package com.catolica.Artepax.common.exception;

import com.catolica.Artepax.documento.exception.DocumentoOperacionNoPermitidaException;
import com.catolica.Artepax.evento.exception.EventoNotFoundException;
import com.catolica.Artepax.pago.exception.EstadoPagoNoConfiguradoException;
import com.catolica.Artepax.pago.exception.MetodoPagoDuplicadoException;
import com.catolica.Artepax.pago.exception.PagoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleArtepaxExceptionRetornaStatusYMensajeDeDominio() {
        ResponseEntity<ErrorResponse> response = handler.handleArtepaxException(new EventoNotFoundException(15L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().codigo());
        assertEquals("Evento no encontrado: 15", response.getBody().mensaje());
    }

    @Test
    void handleArtepaxExceptionRetornaBadRequestParaReglaDeNegocio() {
        ResponseEntity<ErrorResponse> response = handler.handleArtepaxException(
                new PagoException("Solo se pueden confirmar pagos en estado PENDIENTE"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().codigo());
        assertEquals("Solo se pueden confirmar pagos en estado PENDIENTE", response.getBody().mensaje());
    }

    @Test
    void handleArtepaxExceptionRetornaUnprocessableEntityParaOperacionDocumento() {
        ResponseEntity<ErrorResponse> response = handler.handleArtepaxException(
                new DocumentoOperacionNoPermitidaException("No se puede reenviar: el cliente no tiene correo registrado."));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(422, response.getBody().codigo());
    }

    @Test
    void handleArtepaxExceptionRetornaConflictParaDuplicado() {
        ResponseEntity<ErrorResponse> response = handler.handleArtepaxException(
                new MetodoPagoDuplicadoException("TRANSFERENCIA"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().codigo());
    }

    @Test
    void handleArtepaxExceptionRetornaInternalServerErrorParaCatalogoInternoInconsistente() {
        ResponseEntity<ErrorResponse> response = handler.handleArtepaxException(
                new EstadoPagoNoConfiguradoException("PENDIENTE"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().codigo());
    }
}
