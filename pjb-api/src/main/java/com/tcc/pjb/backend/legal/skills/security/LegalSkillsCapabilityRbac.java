package com.tcc.pjb.backend.legal.skills.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.legal.skills.v3.LegalSkillRequestV3;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;

@Component("legalSkillsCapabilityRbac")
public class LegalSkillsCapabilityRbac {

    private final LegalSkillsCapabilityCatalog catalog;

    public LegalSkillsCapabilityRbac(LegalSkillsCapabilityCatalog catalog) {
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
            "ROLE_JUIZ_DELEGADO"
    );

    private static final Set<String> PRAZO_ANY = Set.of(
            "ROLE_ADMIN",
            "ROLE_ADVOGADO",
            "ROLE_SERVIDOR",
            "ROLE_SERVIDOR_FORUM",
            "ROLE_JUIZ",
            "ROLE_DESEMBARGADOR",
            "ROLE_MINISTRO",
            "ROLE_JUIZ_DELEGADO"
    );

    private static final Set<String> RISK_ANY = Set.of(
            "ROLE_ADMIN",
            "ROLE_ADVOGADO",
            "ROLE_SERVIDOR",
            "ROLE_SERVIDOR_FORUM",
            "ROLE_AUDITOR",
            "ROLE_JUIZ",
            "ROLE_DESEMBARGADOR",
            "ROLE_MINISTRO",
            "ROLE_JUIZ_DELEGADO"
    );

    public boolean canExecute(Authentication authentication, LegalSkillRequestV3 request) {
        if (!isAuthenticated(authentication) || request == null) return false;

        
        if (!hasAny(authentication, BASELINE_ANY)) return false;

        String skill = CapabilityStrings.canonical(request.getSkill());
        if (!catalog.isAllowed(skill)) return false;

        if (skill.startsWith("PRAZO")) {
            return hasAny(authentication, PRAZO_ANY);
        }

        if (skill.startsWith("RISK") || skill.startsWith("RISCO")) {
            return hasAny(authentication, RISK_ANY);
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
}
