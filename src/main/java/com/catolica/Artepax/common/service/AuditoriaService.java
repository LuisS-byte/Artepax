package com.catolica.Artepax.common.service;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import com.catolica.Artepax.common.entity.Auditoria;
import com.catolica.Artepax.common.repository.AuditoriaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Servicio para el registro centralizado de auditoría.
 * <p>
 * Persiste un registro en la tabla {@code auditoria} cada vez que una operación
 * relevante es detectada por {@link com.catolica.Artepax.common.aspect.AuditoriaAspect}
 * (u otros invocadores programáticos). Resuelve el usuario autenticado desde el
 * contexto de seguridad y asegura que los valores anteriores/nuevos sean JSON válido
 * para la columna jsonb.
 * <p>
 * Los errores de persistencia se registran en log pero no se propagan, para no
 * interrumpir la operación principal.
 */
@Service
public class AuditoriaService {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            UsuarioRepository usuarioRepository,
                            ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra un evento de auditoría con valores como String JSON.
     * <p>
     * Asegura que los valores anteriores y nuevos sean JSON válido mediante
     * {@link #asegurarJson(String)} antes de persistir. Resuelve el usuario
     * autenticado desde el contexto de seguridad.
     *
     * @param accion        la acción realizada (ver {@link com.catolica.Artepax.common.constant.AuditoriaAcciones})
     * @param tablaAfectada la tabla/entidad afectada (ver {@link com.catolica.Artepax.common.constant.AuditoriaTablas})
     * @param idRegistro    el ID del registro afectado
     * @param valorAnterior snapshot del estado anterior (JSON o null si es creación)
     * @param valorNuevo    snapshot del estado posterior (JSON o null si es eliminación)
     * @param descripcion   descripción legible del evento
     */
    public void registrar(String accion, String tablaAfectada, Long idRegistro,
                          String valorAnterior, String valorNuevo, String descripcion) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setAccion(accion);
            auditoria.setTablaAfectada(tablaAfectada);
            auditoria.setIdRegistro(idRegistro);
            auditoria.setValorAnterior(asegurarJson(valorAnterior));
            auditoria.setValorNuevo(asegurarJson(valorNuevo));
            auditoria.setDescripcion(descripcion);

            Usuario usuario = resolverUsuarioActual();
            if (usuario != null) {
                auditoria.setUsuario(usuario);
            }

            auditoriaRepository.save(auditoria);

        } catch (Exception e) {
            log.error("Error al registrar auditoría [{} en {}]: {}",
                    accion, tablaAfectada, e.getMessage());
        }
    }

    /**
     * Registra un evento de auditoría serializando objetos automáticamente a JSON.
     * <p>
     * Sobrecarga de {@link #registrar(String, String, Long, String, String, String)}
     * que acepta objetos directamente y los serializa internamente.
     *
     * @param accion        la acción realizada
     * @param tablaAfectada la tabla/entidad afectada
     * @param idRegistro    el ID del registro afectado
     * @param valorAnterior objeto con el estado anterior (se serializa a JSON)
     * @param valorNuevo    objeto con el estado posterior (se serializa a JSON)
     * @param descripcion   descripción legible del evento
     */
    public void registrar(String accion, String tablaAfectada, Long idRegistro,
                          Object valorAnterior, Object valorNuevo, String descripcion) {
        registrar(accion, tablaAfectada, idRegistro,
                toJson(valorAnterior), toJson(valorNuevo), descripcion);
    }

    /**
     * Resuelve el usuario autenticado desde el contexto de seguridad.
     *
     * @return el {@link Usuario} autenticado, o {@code null} si no hay sesión activa
     */
    private Usuario resolverUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }

        return usuarioRepository.findByCorreo(auth.getName()).orElse(null);
    }

    /**
     * Asegura que el valor sea JSON válido para columnas jsonb.
     * Si ya es JSON válido (objeto, arreglo, número, booleano), lo pasa sin cambios.
     * Si es texto plano, lo envuelve como JSON string.
     *
     * @param valor el valor a normalizar
     * @return el valor como JSON válido, o {@code null} si la entrada es {@code null}
     */
    private String asegurarJson(String valor) {
        if (valor == null) return null;
        try {
            objectMapper.readTree(valor);
            return valor;
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(valor);
            } catch (JsonProcessingException ex) {
                return "\"" + valor.replace("\"", "\\\"") + "\"";
            }
        }
    }

    /**
     * Serializa un objeto a JSON string.
     *
     * @param obj el objeto a serializar
     * @return el JSON string, o {@code null} si la entrada es {@code null}
     */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar auditoría: {}", e.getMessage());
            return "{\"error\":\"serializacion\"}";
        }
    }
}
