package com.catolica.Artepax.common.aspect;


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

    // CREAR — no hay valorAnterior, @AfterReturning es suficiente
    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.crearEvento(..))",
            returning = "resultado")
    public void auditarCrearEvento(JoinPoint jp, Object resultado) {
        try {
            EventoResponseDTO dto = (EventoResponseDTO) resultado;
            auditoriaService.registrar(
                    "CREAR", "evento", dto.getId(),
                    null,
                    toJson(resultado),
                    "Nuevo evento creado para cliente: " + dto.getClienteNombre()
            );
        } catch (Exception e) {
            log.error("Error en aspecto crearEvento: {}", e.getMessage());
        }
    }

    // ACTUALIZAR — captura estado anterior ANTES de ejecutar
    @Around("execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.actualizarEvento(..))")
    public Object auditarActualizarEvento(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String valorAnterior = eventoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            auditoriaService.registrar(
                    "EDITAR", "evento", id,
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

   //CANCELAR — captura estado anterior ANTES de ejecutar
    @Around("execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.iniciarCancelacion(..))")
    public Object auditarCancelarEvento(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String motivo = (String) pjp.getArgs()[1];
            String valorAnterior = eventoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            auditoriaService.registrar(
                    "CANCELAR", "evento", id,
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

    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.revertirCancelacion(..))",
            returning = "resultado")
    public void auditarRevertirCancelacion(JoinPoint jp, Object resultado) {
        try {
            EventoResponseDTO dto = (EventoResponseDTO) resultado;
            auditoriaService.registrar(
                    "EDITAR", "evento", dto.getId(),
                    null,
                    toJson(resultado),
                    "Cancelación revertida — evento restaurado a estado anterior"
            );
        } catch (Exception e) {
            log.error("Error en aspecto revertirCancelacion: {}", e.getMessage());
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.evento.service.impl.EventoServiceImpl.finalizarEvento(..))",
            returning = "resultado")
    public void auditarFinalizarEvento(JoinPoint jp, Object resultado) {
        try {
            EventoResponseDTO dto = (EventoResponseDTO) resultado;
            auditoriaService.registrar(
                    "FINALIZAR", "evento", dto.getId(),
                    null,
                    toJson(resultado),
                    "Evento finalizado — movimientos de salida generados"
            );
        } catch (Exception e) {
            log.error("Error en aspecto finalizarEvento: {}", e.getMessage());
        }
    }

    // ─── PAGOS ────────────────────────────────────────────────────────────────

    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.pago.service.impl.PagoServiceImpl.registrarPago(..))",
            returning = "resultado")
    public void auditarRegistrarPago(JoinPoint jp, Object resultado) {
        try {
            PagoResponseDTO dto = (PagoResponseDTO) resultado;
            auditoriaService.registrar(
                    "CREAR", "pago", dto.getId(),
                    null,
                    toJson(resultado),
                    "Pago registrado por $" + dto.getMonto()
            );
        } catch (Exception e) {
            log.error("Error en aspecto registrarPago: {}", e.getMessage());
        }
    }

    // CONFIRMAR PAGO — captura estado PENDIENTE antes de cambiar a PAGADO
    @Around("execution(* com.catolica.Artepax.pago.service.impl.PagoServiceImpl.confirmarPago(..))")
    public Object auditarConfirmarPago(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String valorAnterior = pagoRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            PagoResponseDTO dto = (PagoResponseDTO) resultado;
            auditoriaService.registrar(
                    "CONFIRMAR", "pago", dto.getId(),
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

    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.pago.service.impl.PagoServiceImpl.anularPago(..))",
            returning = "resultado")
    public void auditarAnularPago(JoinPoint jp, Object resultado) {
        try {
            PagoResponseDTO dto = (PagoResponseDTO) resultado;
            auditoriaService.registrar(
                    "ANULAR", "pago", dto.getId(),
                    null,
                    toJson(resultado),
                    "Pago anulado"
            );
        } catch (Exception e) {
            log.error("Error en aspecto anularPago: {}", e.getMessage());
        }
    }

    // ─── CLIENTES ─────────────────────────────────────────────────────────────

    @AfterReturning(
            pointcut = "execution(* com.catolica.Artepax.clientes.service.impl.ClienteServiceImpl.crearCliente(..))",
            returning = "resultado")
    public void auditarCrearCliente(JoinPoint jp, Object resultado) {
        try {
            ClienteResponseDTO dto = (ClienteResponseDTO) resultado;
            auditoriaService.registrar(
                    "CREAR", "cliente", dto.getId(),
                    null,
                    toJson(resultado),
                    "Nuevo cliente registrado: " + dto.getNombre()
            );
        } catch (Exception e) {
            log.error("Error en aspecto crearCliente: {}", e.getMessage());
        }
    }

    // ACTUALIZAR CLIENTE — captura estado anterior ANTES de ejecutar
    @Around("execution(* com.catolica.Artepax.clientes.service.impl.ClienteServiceImpl.actualizar(..))")
    public Object auditarActualizarCliente(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Long id = (Long) pjp.getArgs()[0];
            String valorAnterior = clienteRepository.findById(id)
                    .map(this::toJson).orElse(null);

            Object resultado = pjp.proceed();

            ClienteResponseDTO dto = (ClienteResponseDTO) resultado;
            auditoriaService.registrar(
                    "EDITAR", "cliente", dto.getId(),
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

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}