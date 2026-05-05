package com.catolica.Artepax.evento.exception;

/**
 * Excepción de dominio para operaciones de gasto de evento.
 * <p>
 * Se lanza cuando un gasto no puede crearse, desembolsarse o eliminarse
 * por reglas de negocio (estado inválido, gasto ya desembolsado, etc.).
 */
public class GastoEventoException extends RuntimeException {

    public GastoEventoException(String message) {
        super(message);
    }

    public GastoEventoException(String message, Throwable cause) {
        super(message, cause);
    }
}
