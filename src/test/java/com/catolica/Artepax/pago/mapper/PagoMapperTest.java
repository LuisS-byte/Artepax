package com.catolica.Artepax.pago.mapper;

import com.catolica.Artepax.clientes.entity.Cliente;
import com.catolica.Artepax.evento.model.EstadoEvento;
import com.catolica.Artepax.evento.model.Evento;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.entity.ConceptoPago;
import com.catolica.Artepax.pago.entity.EstadoPago;
import com.catolica.Artepax.pago.entity.MetodoPago;
import com.catolica.Artepax.pago.entity.Pago;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PagoMapperTest {

    private final PagoMapper mapper = new PagoMapper();

    @Test
    void toDtoMapeaRelacionesCuandoExisten() {
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("250.00"));

        Evento evento = new Evento();
        evento.setId(9L);

        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Demo");
        evento.setCliente(cliente);

        EstadoEvento estadoEvento = new EstadoEvento();
        estadoEvento.setNombre("CONFIRMADO");
        evento.setEstadoEvento(estadoEvento);
        pago.setEvento(evento);

        EstadoPago estadoPago = new EstadoPago();
        estadoPago.setEstadoNombre("PAGADO");
        pago.setEstadoPago(estadoPago);

        ConceptoPago conceptoPago = new ConceptoPago();
        conceptoPago.setConceptoNombre("ANTICIPO");
        pago.setConceptoPago(conceptoPago);

        MetodoPago metodoPago = new MetodoPago();
        metodoPago.setMetodoNombre("TRANSFERENCIA");
        pago.setMetodoPago(metodoPago);

        PagoResponseDTO dto = mapper.toDTO(pago);

        assertEquals(9L, dto.getEventoId());
        assertEquals("Cliente Demo", dto.getClienteNombre());
        assertEquals("CONFIRMADO", dto.getEstadoEventoNombre());
        assertEquals("PAGADO", dto.getEstadoPago());
        assertEquals("ANTICIPO", dto.getConceptoPago());
        assertEquals("TRANSFERENCIA", dto.getMetodoPago());
    }

    @Test
    void toDtoNoFallaSiEventoEsNull() {
        Pago pago = new Pago();
        pago.setMonto(new BigDecimal("250.00"));

        PagoResponseDTO dto = mapper.toDTO(pago);

        assertNull(dto.getEventoId());
        assertNull(dto.getClienteNombre());
        assertNull(dto.getEstadoEventoNombre());
    }
}
