package com.catolica.Artepax.evento.service;

import com.catolica.Artepax.evento.dto.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Servicio principal para la gestión del ciclo de vida de eventos.
 * <p>
 * Orquesta las transiciones de estado (COTIZACION → PROGRAMADO → CONFIRMADO →
 * EN_MONTAJE → FINALIZADO / PENDIENTE_COBRO → FINALIZADO), la reserva de stock,
 * validación de anticipos, cancelaciones con tiempo de gracia y consultas de rentabilidad.
 */
public interface EventoService {

    /**
     * Crea un nuevo evento en estado COTIZACION.
     *
     * @param requestDTO datos de entrada del evento
     * @return DTO con el evento creado
     */
    EventoResponseDTO crearEvento(EventoRequestDTO requestDTO);

    /**
     * Retorna todos los eventos del sistema.
     */
    List<EventoResponseDTO> obtenerEventos();

    /**
     * Retorna un evento por su identificador.
     *
     * @param id identificador del evento
     * @return DTO del evento
     */
    EventoResponseDTO obtenerEventoPorId(Long id);

    /**
     * Actualiza los datos de un evento.
     * <p>
     * En estado {@code COTIZACION} permite edición completa (todos los campos).
     * En estado {@code PROGRAMADO} permite edición parcial: fecha, horario, ubicación,
     * decoración, nota y notaPrecio — con re-validación de disponibilidad si cambian
     * datos de agenda. No permite cambiar cliente.
     *
     * @param id identificador del evento
     * @param updateDTO datos a actualizar
     * @return DTO del evento actualizado
     */
    EventoResponseDTO actualizarEvento(Long id, EventoUpdateDTO updateDTO);

    /**
     * Agrega productos extra a un evento operativo sin reabrir la edición general.
     */
    EventoResponseDTO agregarExtras(Long id, EventoExtraRequestDTO requestDTO);

    /**
     * Verifica si existe disponibilidad en una fecha, hora y ubicación.
     */
    boolean verificarDisponibilidad(LocalDate fecha,
                                    LocalTime horaInicio,
                                    LocalTime horaFin,
                                    String ubicacion,
                                    Long eventoIdExcluir);

    /**
     * Retorna los eventos dentro de un rango de fechas para el calendario.
     */
    List<CalendarioEventoDTO> obtenerCalendario(LocalDate inicio, LocalDate fin);

    /**
     * Transición: COTIZACION → PROGRAMADO. Reserva stock de productos.
     *
     * @param id identificador del evento
     * @return DTO del evento actualizado
     */
    EventoResponseDTO programarEvento(Long id);

    /**
     * Transición: CONFIRMADO → EN_MONTAJE.
     *
     * @param id identificador del evento
     * @return DTO del evento actualizado
     */
    EventoResponseDTO iniciarMontaje(Long id);

    /**
     * Transición: EN_MONTAJE → FINALIZADO o PENDIENTE_COBRO.
     * <p>
     * Consume el stock de inventario. Si el evento está completamente pago,
     * transiciona a FINALIZADO. Si tiene saldo pendiente, transiciona a
     * PENDIENTE_COBRO para permitir pagos posteriores.
     *
     * @param id identificador del evento
     * @return DTO del evento actualizado
     */
    EventoResponseDTO finalizarEvento(Long id);

    /**
     * Transición automática: PENDIENTE_COBRO → FINALIZADO.
     * <p>
     * Disparada desde el módulo de pagos cuando se completa el cobro total
     * de un evento en PENDIENTE_COBRO. No tiene efecto si el evento no está
     * en PENDIENTE_COBRO o si el cobro aún no está completo.
     *
     * @param id identificador del evento
     */
    void finalizarDesdeCobroCompleto(Long id);

    /**
     * Inicia proceso de cancelación. Si el evento está en COTIZACION, cancela directamente.
     * Para otros estados, pasa a CANCELACION_PENDIENTE con tiempo de gracia.
     *
     * @param id identificador del evento
     * @param motivo razón de la cancelación
     * @return DTO del evento actualizado
     */
    EventoResponseDTO iniciarCancelacion(Long id, String motivo);

    /**
     * Revierte una cancelación pendiente, restaurando el estado anterior.
     *
     * @param id identificador del evento
     * @return DTO del evento actualizado
     */
    EventoResponseDTO revertirCancelacion(Long id);

    /**
     * Confirma la cancelación definitiva: CANCELACION_PENDIENTE → CANCELADO.
     * Cancela el cronograma asociado.
     *
     * @param id identificador del evento
     */
    void confirmarCancelacion(Long id);

    /**
     * Confirmación automática desde el módulo de pagos cuando se alcanza el anticipo mínimo.
     *
     * @param id identificador del evento
     */
    void confirmarDesdeAnticipo(Long id);

    /**
     * Transición: PROGRAMADO → CONFIRMADO. Valida que se haya alcanzado el anticipo mínimo.
     *
     * @param id identificador del evento
     * @return DTO del evento actualizado
     */
    EventoResponseDTO confirmarEvento(Long id);

    /**
     * Retorna la rentabilidad calculada de un evento usando la vista de base de datos.
     *
     * @param id identificador del evento
     * @return DTO con datos de rentabilidad
     */
    RentabilidadEventoDTO getRentabilidad(Long id);

    /**
     * Retorna un resumen interno del evento para uso administrativo.
     */
    EventoResumenInternoDTO getResumenInterno(Long id);

    /**
     * Retorna los productos asociados a un evento.
     * <p>
     * En estado COTIZACION devuelve la composicion de la decoracion
     * (proyeccion sin reserva real). En estados PROGRAMADO en adelante
     * devuelve las reservas efectivas de evento_producto.
     *
     * @param id identificador del evento
     * @return lista de productos con cantidad, costo y origen
     */
    List<EventoProductoDTO> obtenerProductosPorEvento(Long id);
}
