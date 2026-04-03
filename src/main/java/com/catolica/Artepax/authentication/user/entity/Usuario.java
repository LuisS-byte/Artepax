package com.catolica.Artepax.authentication.user.entity;

import jakarta.persistence.*;
import lombok.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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
    private Integer idUsuario;

    @Column(name = "usu_nombre", nullable = false)
    private String nombre;

    @Column(name = "usu_correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "usu_password")
    private String password;

    @Column(name = "usu_telefono")
    private String telefono;

    @Column(name = "estado")
    private Boolean estado;

    // 🔥 RELACIÓN CON ROL (YA CORRECTA)
    @ManyToOne
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
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
        return estado != null ? estado : true;
    }
}