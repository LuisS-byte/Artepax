package com.catolica.Artepax.authentication.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * JPA entity representing a system user ({@code usuario} table).
 * <p>
 * Implements {@link UserDetails} for Spring Security integration.
 * Supports two authentication types: {@code LOCAL} (email+password with BCrypt)
 * and {@code GOOGLE} (Google OAuth). The {@link #estado} flag controls
 * whether the account is active.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    /** Full name of the user. */
    @Column(name = "usu_nombre", nullable = false)
    private String nombre;

    /** Email address — used as the Spring Security username. */
    @Column(name = "usu_correo", nullable = false)
    private String correo;

    /** BCrypt-hashed password (nullable for GOOGLE users). */
    @Column(name = "usu_password")
    private String password;

    /** Optional phone number. */
    @Column(name = "usu_telefono")
    private String telefono;

    /** Authentication type: {@code "LOCAL"} or {@code "GOOGLE"}. Defaults to {@code "LOCAL"}. */
    @Column(name = "usu_tipo_auth", nullable = false, length = 10)
    @Builder.Default
    private String tipoAuth = "LOCAL";

    /** Whether the user account is active. Inactive users cannot authenticate. */
    @Column(name = "estado")
    private Boolean estado;

    /** Timestamp of account creation — set automatically by Hibernate. */
    @CreationTimestamp
    @Column(name = "usu_fechacreacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** The user's role (e.g. ADMIN, EMPLEADO). */
    @ManyToOne
    @JoinColumn(name = "id_rol")
    private Rol rol;

    /**
     * {@inheritDoc}
     * <p>
     * Returns a single authority with the format {@code "ROLE_<nombre>"}
     * based on the user's {@link Rol}. If the role is {@code null},
     * an empty list is returned.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (rol == null) return List.of();
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.getNombre()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The username is the user's email address ({@link #correo}).
     */
    @Override
    public String getUsername() {
        return this.correo;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the BCrypt-hashed password. May be {@code null} for
     * users authenticated via Google OAuth ({@code tipoAuth = "GOOGLE"}).
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /** {@inheritDoc} */
    @Override public boolean isAccountNonExpired() { return true; }
    /** {@inheritDoc} */
    @Override public boolean isAccountNonLocked() { return true; }
    /** {@inheritDoc} */
    @Override public boolean isCredentialsNonExpired() { return true; }
    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the {@link #estado} field — only active users
     * are considered enabled.
     */
    @Override public boolean isEnabled() {
        return Boolean.TRUE.equals(estado);
    }
}