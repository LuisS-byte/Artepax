package com.catolica.Artepax.documento.service.policy;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.documento.entity.Documento;
import com.catolica.Artepax.documento.entity.DocumentoEstadoEnvio;
import com.catolica.Artepax.documento.entity.DocumentoTipo;
import com.catolica.Artepax.documento.entity.PlantillaDocumento;
import com.catolica.Artepax.documento.exception.DocumentoOperacionNoPermitidaException;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.EstadoEventoNames;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.evento.model.TipoEvento;
import com.catolica.Artepax.inventario.decoracion.entity.Decoracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoPolicyTest {

    private DocumentoPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DocumentoPolicy();
    }

    // AVISO_SALDO_PENDIENTE - estados permitidos

    @Test
    void generarAvisoSaldoPendiente_enProgramado_permitido() {
        Evento evento = crearEventoConEstado(EstadoEventoNames.PROGRAMADO);
        assertDoesNotThrow(() -> policy.validarGeneracionPermitida(evento, DocumentoTipo.AVISO_SALDO_PENDIENTE));
    }

    @Test
    void generarAvisoSaldoPendiente_enConfirmado_permitido() {
        Evento evento = crearEventoConEstado(EstadoEventoNames.CONFIRMADO);
        assertDoesNotThrow(() -> policy.validarGeneracionPermitida(evento, DocumentoTipo.AVISO_SALDO_PENDIENTE));
    }

    @Test
    void generarAvisoSaldoPendiente_enMontaje_permitido() {
        Evento evento = crearEventoConEstado(EstadoEventoNames.EN_MONTAJE);
        assertDoesNotThrow(() -> policy.validarGeneracionPermitida(evento, DocumentoTipo.AVISO_SALDO_PENDIENTE));
    }

    @Test
    void generarAvisoSaldoPendiente_enFinalizado_permitido() {
        Evento evento = crearEventoConEstado(EstadoEventoNames.FINALIZADO);
        assertDoesNotThrow(() -> policy.validarGeneracionPermitida(evento, DocumentoTipo.AVISO_SALDO_PENDIENTE));
    }

    // AVISO_SALDO_PENDIENTE - estados no permitidos

    @Test
    void generarAvisoSaldoPendiente_enCotizacion_lanzaError() {
        Evento evento = crearEventoConEstado(EstadoEventoNames.COTIZACION);
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarGeneracionPermitida(evento, DocumentoTipo.AVISO_SALDO_PENDIENTE));
    }

    @Test
    void generarAvisoSaldoPendiente_enCancelacionPendiente_lanzaError() {
        Evento evento = crearEventoConEstado(EstadoEventoNames.CANCELACION_PENDIENTE);
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarGeneracionPermitida(evento, DocumentoTipo.AVISO_SALDO_PENDIENTE));
    }

    @Test
    void generarAvisoSaldoPendiente_enCancelado_lanzaError() {
        Evento evento = crearEventoConEstado(EstadoEventoNames.CANCELADO);
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarGeneracionPermitida(evento, DocumentoTipo.AVISO_SALDO_PENDIENTE));
    }

    // validarSaldoPendiente

    @Test
    void validarSaldoPendiente_conSaldoPositivo_permitido() {
        assertDoesNotThrow(() -> policy.validarSaldoPendiente(new BigDecimal("500.00")));
    }

    @Test
    void validarSaldoPendiente_conSaldoCero_lanzaError() {
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarSaldoPendiente(BigDecimal.ZERO));
    }

    @Test
    void validarSaldoPendiente_conSaldoNegativo_lanzaError() {
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarSaldoPendiente(new BigDecimal("-100.00")));
    }

    @Test
    void validarSaldoPendiente_conNull_lanzaError() {
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarSaldoPendiente(null));
    }

    // validarReenvioPermitido

    @Test
    void validarReenvio_conCorreoyMenosDe3Intentos_permitido() {
        Documento doc = crearDocumentoConIntentos("cliente@test.com", 1, true);
        assertDoesNotThrow(() -> policy.validarReenvioPermitido(doc));
    }

    @Test
    void validarReenvio_con3Intentos_lanzaError() {
        Documento doc = crearDocumentoConIntentos("cliente@test.com", 3, true);
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarReenvioPermitido(doc));
    }

    @Test
    void validarReenvio_sinCorreo_lanzaError() {
        Documento doc = crearDocumentoConIntentos(null, 1, true);
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarReenvioPermitido(doc));
    }

    // validarPlantillaDisponible

    @Test
    void validarPlantilla_activa_permitido() {
        PlantillaDocumento plantilla = new PlantillaDocumento();
        plantilla.setActivo(true);
        assertDoesNotThrow(() -> policy.validarPlantillaDisponible(plantilla));
    }

    @Test
    void validarPlantilla_inactiva_lanzaError() {
        PlantillaDocumento plantilla = new PlantillaDocumento();
        plantilla.setActivo(false);
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarPlantillaDisponible(plantilla));
    }

    @Test
    void validarPlantilla_null_lanzaError() {
        assertThrows(DocumentoOperacionNoPermitidaException.class,
                () -> policy.validarPlantillaDisponible(null));
    }

    // Helpers

    private Evento crearEventoConEstado(String estadoNombre) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Demo");
        cliente.setCorreo("cliente@test.com");

        EstadoEvento estado = new EstadoEvento();
        estado.setNombre(estadoNombre);

        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setTipoNombre("BODA");

        Decoracion decoracion = new Decoracion();
        decoracion.setNombre("Clasica");

        Evento evento = new Evento();
        evento.setId(1L);
        evento.setCliente(cliente);
        evento.setEstadoEvento(estado);
        evento.setTipoEvento(tipoEvento);
        evento.setDecoracion(decoracion);
        return evento;
    }

    private Documento crearDocumentoConIntentos(String correo, int intentos, boolean conPlantilla) {
        Documento doc = new Documento();
        doc.setIdDocumento(1L);
        doc.setEvento(crearEventoConEstado(EstadoEventoNames.CONFIRMADO));
        doc.getEvento().getCliente().setCorreo(correo);
        doc.setDocuTipo(DocumentoTipo.CONFIRMACION_RESERVA);
        doc.setDocuEstadoEnvio(DocumentoEstadoEnvio.FALLIDO);
        doc.setDocuIntentosEnvio(intentos);
        if (conPlantilla) {
            PlantillaDocumento plantilla = new PlantillaDocumento();
            plantilla.setActivo(true);
            doc.setPlantilla(plantilla);
        }
        return doc;
    }
}
