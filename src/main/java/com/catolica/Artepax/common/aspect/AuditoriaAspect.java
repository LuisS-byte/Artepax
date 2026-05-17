package com.catolica.Artepax.common.aspect;


import com.catolica.Artepax.common.constant.AuditoriaAcciones;
import com.catolica.Artepax.common.constant.AuditoriaTablas;
import com.catolica.Artepax.common.service.AuditoriaService;
import com.catolica.Artepax.evento.dto.EventoResponseDTO;
import com.catolica.Artepax.evento.repository.EventoRepository;
import com.catolica.Artepax.clientes.dto.ClienteResponseDTO;
import com.catolica.Artepax.clientes.repository.ClienteRepository;
import com.catolica.Artepax.pago.dto.PagoResponseDTO;
import com.catolica.Artepax.pago.repository.PagoRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Aspecto AOP que audita automáticamente operaciones CRUD y de negocio
 * sobre Eventos, Pagos y Clientes.
 * <p>
 * Cada método de este aspecto es un advice ({@code @AfterReturning} o
 * {@code @Around}) cuyo pointcut está atado a la firma completa del método
 * en la implementación concreta del servicio ({@code EventoServiceImpl},
 * {@code PagoServiceImpl}, {@code ClienteServiceImpl}).
 * <p>
 * <strong>⚠️ ADVERTENCIA — Pointcuts frágiles:</strong> renombrar, cambiar
 * la firma (parámetros o tipos de retorno), o mover los métodos auditados
 * entre clases ROMperá el advice SILENCIOSAMENTE. El aspecto atrapa sus
 * propias excepciones y solo las loguea, por lo que un pointcut que no
 * matchea no produce error de compilación ni startup — simplemente deja
 * de auditar esa operación sin aviso.
 * <p>
 * Los advices {@code @Around} capturan el estado anterior ({@code valorAnterior})
 * antes de ejecutar el método objetivo y el estado posterior después. Los
 * advices {@code @AfterReturning} solo registran el estado posterior (no
 * necesitan el valor anterior porque la operación es de creación o el estado
 * anterior no es relevante).
 */
@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;
    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final PagoRepository pagoRepository;

    public AuditoriaAspect(AuditoriaService auditoriaService,
                           ObjectMapper objectMapper,
                           EventoRepository eventoRepository,
                           ClienteRepository clienteRepository,
                           PagoRepository pagoRepository) {
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
        this.eventoRepository = eventoRepository;
        this.clienteRepository = clienteRepository;
        this.pagoRepository = pagoRepository;
    }

    // ─── EVENTOS ─────────────────────────────────────────────────────────────

    /**
     * Audita la creación de un evento.
     * Pointcut: {@code EventoServiceImpl.crearEvento(..)}.
     * Usa {@code @AfterReturning} porque no hay valor anterior (es creación).
     *
     * @param jp       el join point (no se usa directamente)
     * @param resultado el DTO {@link EventoResponseDTO} retornado por el método
     */
    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.crearEvento(..))",
            returning = "resultado")
    public void auditarCrearEvento(JoinPoint jp, Object resultado) {
        try {
            EventoResponseDTO dto = (EventoResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.CREAR, AuditoriaTablas.EVENTO, dto.getId(),
                    null,
                    toJson(resultado),
                    "Nuevo evento creado para cliente: " + dto.getClienteNombre()
            );
        } catch (Exception e) {
            log.error("Error en aspecto crearEvento: {}", e.getMessage());
        }
    }

    /**
     * Audita la actualización de un evento.
     * Pointcut: {@code EventoServiceImpl.actualizarEvento(..)}.
     * Usa {@code @Around} para capturar el estado anterior del evento antes
     * de aplicar los cambios.
     *
     * @param pjp el proceeding join point para ejecutar el método objetivo
     * @return el resultado del método objetivo ({@link EventoResponseDTO})
     * @throws Throwable si el método objetivo lanza una excepción
     */
    @Around("execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.actualizarEvento(..))")
    public Object auditarActualizarEvento(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String valorAnterior = eventoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            auditoriaService.registrar(
                    AuditoriaAcciones.EDITAR, AuditoriaTablas.EVENTO, id,
                    valorAnterior,
                    toJson(resultado),
                    "Evento actualizado"
            );
            return resultado;
        } catch (Exception e) {
            log.error("Error en aspecto actualizarEvento: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Audita el inicio de cancelación de un evento.
     * Pointcut: {@code EventoServiceImpl.iniciarCancelacion(..)}.
     * Captura el estado anterior antes de la cancelación e incluye el motivo.
     *
     * @param pjp el proceeding join point para ejecutar el método objetivo
     * @return el resultado del método objetivo ({@link EventoResponseDTO})
     * @throws Throwable si el método objetivo lanza una excepción
     */
    @Around("execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.iniciarCancelacion(..))")
    public Object auditarCancelarEvento(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String motivo = (String) pjp.getArgs()[1];
            String valorAnterior = eventoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            auditoriaService.registrar(
                    AuditoriaAcciones.CANCELAR, AuditoriaTablas.EVENTO, id,
                    valorAnterior,
                    toJson(resultado),
                    "Cancelación iniciada. Motivo: " + motivo
            );
            return resultado;
        } catch (Exception e) {
            log.error("Error en aspecto iniciarCancelacion: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Audita la reversión de una cancelación de evento.
     * Pointcut: {@code EventoServiceImpl.revertirCancelacion(..)}.
     * Usa {@code @AfterReturning} porque el estado anterior se pierde al revertir.
     *
     * @param jp       el join point (no se usa directamente)
     * @param resultado el DTO {@link EventoResponseDTO} retornado
     */
    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.revertirCancelacion(..))",
            returning = "resultado")
    public void auditarRevertirCancelacion(JoinPoint jp, Object resultado) {
        try {
            EventoResponseDTO dto = (EventoResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.EDITAR, AuditoriaTablas.EVENTO, dto.getId(),
                    null,
                    toJson(resultado),
                    "Cancelación revertida — evento restaurado a estado anterior"
            );
        } catch (Exception e) {
            log.error("Error en aspecto revertirCancelacion: {}", e.getMessage());
        }
    }

    /**
     * Audita la finalización de un evento.
     * Pointcut: {@code EventoServiceImpl.finalizarEvento(..)}.
     * Usa {@code @AfterReturning} porque la finalización genera movimientos de salida.
     *
     * @param jp       el join point (no se usa directamente)
     * @param resultado el DTO {@link EventoResponseDTO} retornado
     */
    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.finalizarEvento(..))",
            returning = "resultado")
    public void auditarFinalizarEvento(JoinPoint jp, Object resultado) {
        try {
            EventoResponseDTO dto = (EventoResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.FINALIZAR, AuditoriaTablas.EVENTO, dto.getId(),
                    null,
                    toJson(resultado),
                    "Evento finalizado — movimientos de salida generados"
            );
        } catch (Exception e) {
            log.error("Error en aspecto finalizarEvento: {}", e.getMessage());
        }
    }

    /**
     * Audita el inicio de montaje de un evento (CONFIRMADO → EN_MONTAJE).
     * Pointcut: {@code EventoServiceImpl.iniciarMontaje(..)}.
     *
     * @param jp       el join point (no se usa directamente)
     * @param resultado el DTO {@link EventoResponseDTO} retornado
     */
    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.iniciarMontaje(..))",
            returning = "resultado")
    public void auditarIniciarMontaje(JoinPoint jp, Object resultado) {
        try {
            EventoResponseDTO dto = (EventoResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.INICIAR_MONTAJE, AuditoriaTablas.EVENTO, dto.getId(),
                    null,
                    toJson(resultado),
                    "Montaje iniciado — evento en EN_MONTAJE"
            );
        } catch (Exception e) {
            log.error("Error en aspecto iniciarMontaje: {}", e.getMessage());
        }
    }

    /**
     * Audita la confirmación definitiva de cancelación (CANCELACION_PENDIENTE → CANCELADO).
     * Pointcut: {@code EventoServiceImpl.confirmarCancelacion(..)}.
     * Usa {@code @Around} porque el método retorna {@code void}, y se debe
     * leer el estado posterior de la base de datos.
     *
     * @param pjp el proceeding join point para ejecutar el método objetivo
     * @return el resultado del método objetivo (puede ser {@code null} si es void)
     * @throws Throwable si el método objetivo lanza una excepción
     */
    @Around("execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.confirmarCancelacion(..))")
    public Object auditarConfirmarCancelacion(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String valorAnterior = eventoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            String valorNuevo = eventoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            auditoriaService.registrar(
                    AuditoriaAcciones.CANCELAR, AuditoriaTablas.EVENTO, id,
                    valorAnterior,
                    valorNuevo,
                    "Cancelación definitiva del evento confirmada"
            );
            return resultado;
        } catch (Exception e) {
            log.error("Error en aspecto confirmarCancelacion: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Audita la finalización automática por cobro completo (PENDIENTE_COBRO → FINALIZADO).
     * Pointcut: {@code EventoServiceImpl.finalizarDesdeCobroCompleto(..)}.
     * Usa {@code @Around} porque el método retorna {@code void}.
     *
     * @param pjp el proceeding join point para ejecutar el método objetivo
     * @return el resultado del método objetivo (puede ser {@code null})
     * @throws Throwable si el método objetivo lanza una excepción
     */
    @Around("execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.finalizarDesdeCobroCompleto(..))")
    public Object auditarFinalizarDesdeCobroCompleto(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String valorAnterior = eventoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            String valorNuevo = eventoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            auditoriaService.registrar(
                    AuditoriaAcciones.FINALIZAR, AuditoriaTablas.EVENTO, id,
                    valorAnterior,
                    valorNuevo,
                    "Evento finalizado automáticamente por cobro completo"
            );
            return resultado;
        } catch (Exception e) {
            log.error("Error en aspecto finalizarDesdeCobroCompleto: {}", e.getMessage());
            throw e;
        }
    }

    // ─── PAGOS ────────────────────────────────────────────────────────────────

    /**
     * Audita el registro de un nuevo pago.
     * Pointcut: {@code PagoServiceImpl.registrarPago(..)}.
     *
     * @param jp       el join point (no se usa directamente)
     * @param resultado el DTO {@link PagoResponseDTO} retornado
     */
    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.pago.service.impl.PagoServiceImpl.registrarPago(..))",
            returning = "resultado")
    public void auditarRegistrarPago(JoinPoint jp, Object resultado) {
        try {
            PagoResponseDTO dto = (PagoResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.CREAR, AuditoriaTablas.PAGO, dto.getId(),
                    null,
                    toJson(resultado),
                    "Pago registrado por $" + dto.getMonto()
            );
        } catch (Exception e) {
            log.error("Error en aspecto registrarPago: {}", e.getMessage());
        }
    }

    /**
     * Audita la confirmación de un pago (PENDIENTE → PAGADO).
     * Pointcut: {@code PagoServiceImpl.confirmarPago(..)}.
     * Captura el estado anterior (PENDIENTE) antes de cambiar a PAGADO.
     *
     * @param pjp el proceeding join point para ejecutar el método objetivo
     * @return el resultado del método objetivo ({@link PagoResponseDTO})
     * @throws Throwable si el método objetivo lanza una excepción
     */
    @Around("execution(* com.catolica.Artepax.pago.service.impl.PagoServiceImpl.confirmarPago(..))")
    public Object auditarConfirmarPago(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String valorAnterior = pagoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            PagoResponseDTO dto = (PagoResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.CONFIRMAR, AuditoriaTablas.PAGO, dto.getId(),
                    valorAnterior,
                    toJson(resultado),
                    "Pago confirmado por $" + dto.getMonto()
            );
            return resultado;
        } catch (Exception e) {
            log.error("Error en aspecto confirmarPago: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Audita la anulación de un pago.
     * Pointcut: {@code PagoServiceImpl.anularPago(..)}.
     *
     * @param jp       el join point (no se usa directamente)
     * @param resultado el DTO {@link PagoResponseDTO} retornado
     */
    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.pago.service.impl.PagoServiceImpl.anularPago(..))",
            returning = "resultado")
    public void auditarAnularPago(JoinPoint jp, Object resultado) {
        try {
            PagoResponseDTO dto = (PagoResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.ANULAR, AuditoriaTablas.PAGO, dto.getId(),
                    null,
                    toJson(resultado),
                    "Pago anulado"
            );
        } catch (Exception e) {
            log.error("Error en aspecto anularPago: {}", e.getMessage());
        }
    }

    // ─── CLIENTES ─────────────────────────────────────────────────────────────

    /**
     * Audita la creación de un nuevo cliente.
     * Pointcut: {@code ClienteServiceImpl.crearCliente(..)}.
     *
     * @param jp       el join point (no se usa directamente)
     * @param resultado el DTO {@link ClienteResponseDTO} retornado
     */
    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.clientes.service.impl.ClienteServiceImpl.crearCliente(..))",
            returning = "resultado")
    public void auditarCrearCliente(JoinPoint jp, Object resultado) {
        try {
            ClienteResponseDTO dto = (ClienteResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.CREAR, AuditoriaTablas.CLIENTE, dto.getId(),
                    null,
                    toJson(resultado),
                    "Nuevo cliente registrado: " + dto.getNombre()
            );
        } catch (Exception e) {
            log.error("Error en aspecto crearCliente: {}", e.getMessage());
        }
    }

    /**
     * Audita la actualización de un cliente.
     * Pointcut: {@code ClienteServiceImpl.actualizar(..)}.
     * Captura el estado anterior del cliente antes de aplicar los cambios.
     *
     * @param pjp el proceeding join point para ejecutar el método objetivo
     * @return el resultado del método objetivo ({@link ClienteResponseDTO})
     * @throws Throwable si el método objetivo lanza una excepción
     */
    @Around("execution(* com.catolica.Artepax.clientes.service.impl.ClienteServiceImpl.actualizar(..))")
    public Object auditarActualizarCliente(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String valorAnterior = clienteRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            ClienteResponseDTO dto = (ClienteResponseDTO) resultado;
            auditoriaService.registrar(
                    AuditoriaAcciones.EDITAR, AuditoriaTablas.CLIENTE, dto.getId(),
                    valorAnterior,
                    toJson(resultado),
                    "Cliente actualizado: " + dto.getNombre()
            );
            return resultado;
        } catch (Exception e) {
            log.error("Error en aspecto actualizarCliente: {}", e.getMessage());
            throw e;
        }
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    /**
     * Serializa un objeto a JSON string usando ObjectMapper.
     * Si falla la serialización, retorna un JSON vacío {@code "{}"} para no
     * interrumpir el registro de auditoría.
     *
     * @param obj el objeto a serializar
     * @return representación JSON del objeto, o {@code "{}"} si hay error
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}