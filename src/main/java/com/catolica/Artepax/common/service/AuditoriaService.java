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

    public void registrar(String accion, String tablaAfectada, Long idRegistro,
                          Object valorAnterior, Object valorNuevo, String descripcion) {
        registrar(accion, tablaAfectada, idRegistro,
                toJson(valorAnterior), toJson(valorNuevo), descripcion);
    }

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
