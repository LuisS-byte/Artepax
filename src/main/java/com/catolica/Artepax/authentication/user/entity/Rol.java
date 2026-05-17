package com.catolica.Artepax.authentication.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing a user role ({@code rol} table).
 * <p>
 * Roles are referenced by {@link Usuario} entities and mapped to
 * Spring Security {@link org.springframework.security.core.GrantedAuthority}
 * via the {@code "ROLE_"} prefix convention in {@link Usuario#getAuthorities()}.
 */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    /** Role name (e.g. {@code "ADMIN"}, {@code "EMPLEADO"}). */
    @Column(name = "rol_nombre", nullable = false)
    private String nombre;

    /** Optional description of the role's permissions or purpose. */
    @Column(name = "rol_descripcion")
    private String descripcion;
}