package com.catolica.Artepax.common.service;

import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.repository.UsuarioRepository;
import com.catolica.Artepax.common.entity.Auditoria;
import com.catolica.Artepax.common.repository.AuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void registrar(String accion, String tablaAfectada, Long idRegistro,
                          String valorAnterior, String valorNuevo, String descripcion) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setAccion(accion);
            auditoria.setTablaAfectada(tablaAfectada);
            auditoria.setIdRegistro(idRegistro);
            auditoria.setValorAnterior(valorAnterior);
            auditoria.setValorNuevo(valorNuevo);
            auditoria.setDescripcion(descripcion);

            // Obtener usuario autenticado desde Spring Security
            String correo = SecurityContextHolder.getContext()
                    .getAuthentication().getName();

            usuarioRepository.findByCorreo(correo).ifPresent(auditoria::setUsuario);

            auditoriaRepository.save(auditoria);

        } catch (Exception e) {
            // La auditoría nunca debe romper el flujo principal
            log.error("Error al registrar auditoría [{} en {}]: {}",
                    accion, tablaAfectada, e.getMessage());
        }
    }
}
