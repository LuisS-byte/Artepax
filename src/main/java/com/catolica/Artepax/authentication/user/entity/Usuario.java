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

    @Column(name = "usu_nombre", nullable = false)
    private String nombre;

    @Column(name = "usu_correo", nullable = false)
    private String correo;

    @Column(name = "usu_password")
    private String password;

    @Column(name = "usu_telefono")
    private String telefono;

    @Column(name = "usu_tipo_auth", nullable = false, length = 10)
    @Builder.Default
    private String tipoAuth = "LOCAL";

    @Column(name = "estado")
    private Boolean estado;

    @CreationTimestamp
    @Column(name = "usu_fechacreacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (rol == null) return List.of();
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.getNombre()));
    }

    @Override
    public String getUsername() {
        return this.correo;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() {
        return Boolean.TRUE.equals(estado);
    }
}