package com.tcc.pjb.backend.configs.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;

    public Long getId() {
        return usuario.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.Collections.unmodifiableList(PjbGrantedAuthorityFactory.authoritiesFor(usuario.getTipoUsuario(), usuario.getEnteFederativo()));
    }

    @Override
    public String getPassword() { return usuario.getSenha(); }
    @Override
    public String getUsername() { return usuario.getEmail(); }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return usuario.isAtivo(); }
}