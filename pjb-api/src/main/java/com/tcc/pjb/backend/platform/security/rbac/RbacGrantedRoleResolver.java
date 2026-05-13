package com.tcc.pjb.backend.platform.security.rbac;

import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Component
public class RbacGrantedRoleResolver {

    private final RbacRoleCatalog roleCatalog;

    public RbacGrantedRoleResolver(RbacRoleCatalog roleCatalog) {
        this.roleCatalog = Objects.requireNonNull(roleCatalog);
    }

    public Set<String> resolveGrantedRoles(TipoUsuario tipoUsuario) {
        return roleCatalog.resolveFor(tipoUsuario);
    }
}
