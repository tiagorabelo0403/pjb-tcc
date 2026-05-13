package com.tcc.pjb.backend.ai.juridica.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;
import com.tcc.pjb.backend.platform.versioning.VersionHints;

@Component("juridicaCapabilityRbac")
public class JuridicaCapabilityRbac {

    private final JuridicaCapabilityCatalog catalog;

    public JuridicaCapabilityRbac(JuridicaCapabilityCatalog catalog) {
        this.catalog = java.util.Objects.requireNonNull(catalog, "catalog");
    }

    private static final Set<String> BASELINE_ANY = Set.of(
            "ROLE_ADMIN",
            "ROLE_ADVOGADO",
            "ROLE_SERVIDOR",
            "ROLE_SERVIDOR_FORUM",
            "ROLE_JUIZ",
            "ROLE_DESEMBARGADOR",
            "ROLE_MINISTRO",
            "ROLE_JUIZ_DELEGADO",
            "ROLE_OAB_PRESIDENTE_SECCIONAL"
    );

    private static final Set<String> MAGISTRATURA_ANY = Set.of(
            "ROLE_ADMIN",
            "ROLE_JUIZ",
            "ROLE_DESEMBARGADOR",
            "ROLE_MINISTRO",
            "ROLE_JUIZ_DELEGADO"
    );

    private static final Set<String> OAB_SECCIONAL_ANY = Set.of(
            "ROLE_ADMIN",
            "ROLE_OAB_PRESIDENTE_SECCIONAL"
    );

    public boolean canExecute(Authentication authentication, IARequest request) {
        if (!isAuthenticated(authentication) || request == null) return false;

        
        if (!hasAny(authentication, BASELINE_ANY)) return false;

        String rawCap = VersionHints.resolveCapability(request.getPayload(), request.getAcao());
        String cap = CapabilityStrings.canonical(rawCap);
        if (!catalog.isAllowed(cap)) return false;

        
        if (matchesAny(cap, "SENTENCA", "SENTENÇA", "ACORDAO", "ACÓRDÃO", "DECISAO", "DECISÃO", "DESPACHO", "VOTO")) {
            return hasAny(authentication, MAGISTRATURA_ANY);
        }

        
        if (matchesAny(cap, "OAB", "SECCIONAL", "INCIDENTE_OAB")) {
            return hasAny(authentication, OAB_SECCIONAL_ANY) || hasAny(authentication, MAGISTRATURA_ANY);
        }

        
        return true;
    }

    private static boolean isAuthenticated(Authentication a) {
        return a != null && a.isAuthenticated();
    }

    private static boolean hasAny(Authentication a, Set<String> requiredAny) {
        Collection<? extends GrantedAuthority> auths = a.getAuthorities();
        if (auths == null || auths.isEmpty()) return false;
        for (GrantedAuthority ga : auths) {
            if (ga == null) continue;
            String v = ga.getAuthority();
            if (v != null && requiredAny.contains(v)) return true;
        }
        return false;
    }

    private static boolean matchesAny(String capCanonical, String... tokens) {
        if (capCanonical == null) return false;
        for (String t : tokens) {
            if (t == null || t.isBlank()) continue;
            String token = CapabilityStrings.canonical(t);
            if (token != null && capCanonical.contains(token)) return true;
        }
        return false;
    }
}
