package com.catolica.Artepax.pago.service.impl;

import com.catolica.Artepax.pago.entity.MetodoPago;
import com.catolica.Artepax.pago.exception.MetodoPagoDuplicadoException;
import com.catolica.Artepax.pago.exception.MetodoPagoNotFoundException;
import com.catolica.Artepax.pago.repository.ConceptoPagoRepository;
import com.catolica.Artepax.pago.repository.EstadoPagoRepository;
import com.catolica.Artepax.pago.repository.MetodoPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogosPagoServiceImplTest {

    @Mock
    private EstadoPagoRepository estadoPagoRepository;

    @Mock
    private ConceptoPagoRepository conceptoPagoRepository;

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    private CatalogosPagoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CatalogosPagoServiceImpl(estadoPagoRepository, conceptoPagoRepository, metodoPagoRepository);
    }

    @Test
    void crearMetodoFallaSiNombreYaExiste() {
        MetodoPago existente = new MetodoPago();
        existente.setMetodoNombre("TRANSFERENCIA");
        when(metodoPagoRepository.findByMetodoNombreIgnoreCase("transferencia")).thenReturn(Optional.of(existente));

        assertThrows(MetodoPagoDuplicadoException.class,
                () -> service.crearMetodo(" transferencia ", "Alias"));
        verify(metodoPagoRepository, never()).save(any(MetodoPago.class));
    }

    @Test
    void toggleMetodoFallaSiNoExiste() {
        when(metodoPagoRepository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(MetodoPagoNotFoundException.class, () -> service.toggleMetodo(8L));
    }

    @Test
    void toggleMetodoInvierteEstado() {
        MetodoPago metodo = new MetodoPago();
        metodo.setMetodoNombre("EFECTIVO");
        metodo.setMetodoActivo(true);
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(metodo));
        when(metodoPagoRepository.save(metodo)).thenReturn(metodo);

        var dto = service.toggleMetodo(2L);

        assertEquals(false, dto.getMetodoActivo());
        verify(metodoPagoRepository).save(metodo);
    }
}
