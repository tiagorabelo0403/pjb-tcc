package com.tcc.pjb.backend.core.security.login;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public sealed interface PerfilLoginStrategy permits
        PerfilLoginStrategy.GovBrOidc,
        PerfilLoginStrategy.OabCredential,
        PerfilLoginStrategy.SiapeCredential,
        PerfilLoginStrategy.SiapeComMatricula,
        PerfilLoginStrategy.SesspCredential,
        PerfilLoginStrategy.PeritoCadastro,
        PerfilLoginStrategy.TribunalInstitucional,
        PerfilLoginStrategy.CejuscPortal,
        PerfilLoginStrategy.CartorioInstitucional,
        PerfilLoginStrategy.AdminInternal {

    TipoUsuario[] targetProfiles();

    String loginEndpointHint();

    boolean requiresMfa();

    boolean requiresDeviceBinding();

    record GovBrOidc(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/govbr/authorize"; }
        @Override public boolean requiresMfa() { return false; }
        @Override public boolean requiresDeviceBinding() { return false; }
    }

    record OabCredential(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/oab/credential"; }
        @Override public boolean requiresMfa() { return true; }
        @Override public boolean requiresDeviceBinding() { return true; }
    }

    record SiapeCredential(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/siape/credential"; }
        @Override public boolean requiresMfa() { return true; }
        @Override public boolean requiresDeviceBinding() { return true; }
    }

    record SiapeComMatricula(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/servidor/matricula"; }
        @Override public boolean requiresMfa() { return true; }
        @Override public boolean requiresDeviceBinding() { return true; }
    }

    record SesspCredential(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/sessp/credential"; }
        @Override public boolean requiresMfa() { return true; }
        @Override public boolean requiresDeviceBinding() { return true; }
    }

    record PeritoCadastro(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/perito/registro"; }
        @Override public boolean requiresMfa() { return true; }
        @Override public boolean requiresDeviceBinding() { return false; }
    }

    record TribunalInstitucional(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/tribunal/institutional"; }
        @Override public boolean requiresMfa() { return true; }
        @Override public boolean requiresDeviceBinding() { return true; }
    }

    record CejuscPortal(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/cejusc/portal"; }
        @Override public boolean requiresMfa() { return false; }
        @Override public boolean requiresDeviceBinding() { return false; }
    }

    record CartorioInstitucional(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/cartorio/institutional"; }
        @Override public boolean requiresMfa() { return true; }
        @Override public boolean requiresDeviceBinding() { return true; }
    }

    record AdminInternal(TipoUsuario[] targetProfiles) implements PerfilLoginStrategy {
        @Override public String loginEndpointHint() { return "/api/v1/auth/admin/internal"; }
        @Override public boolean requiresMfa() { return true; }
        @Override public boolean requiresDeviceBinding() { return true; }
    }
}
