package com.tcc.pjb.backend.modules.laiane.util;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;

@Component
public class LaianeRoleGuard {

    private final CurrentUserService currentUserService;

    public LaianeRoleGuard(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public Usuario requireMagistratura() {
        Usuario u = currentUserService.get();
        
        if (u == null || u.getTipoUsuario() == null || !u.getTipoUsuario().isMagistratura()) {
            throw new SecurityException("Acesso restrito à Magistratura.");
        }
        return u;
    }

    public Usuario requireMinisterioPublico() {
        Usuario u = currentUserService.get();
        
        if (u == null || u.getTipoUsuario() == null || !u.getTipoUsuario().isMinisterioPublico()) {
            throw new SecurityException("Acesso restrito ao Ministério Público.");
        }
        return u;
    }

    public Usuario requireAdvocacia() {
        Usuario u = currentUserService.get();
        
        if (u == null || u.getTipoUsuario() == null || !u.getTipoUsuario().isAdvocacia()) {
            throw new SecurityException("Acesso restrito à Advocacia.");
        }
        return u;
    }

    public Usuario requireAdvogado() {
        return requireAdvocacia();
    }
}
