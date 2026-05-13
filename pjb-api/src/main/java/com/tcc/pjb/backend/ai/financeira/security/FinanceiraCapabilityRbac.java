package com.tcc.pjb.backend.ai.financeira.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;
import com.tcc.pjb.backend.platform.versioning.VersionHints;

@Component("financeiraCapabilityRbac")
public class FinanceiraCapabilityRbac {

    private final FinanceiraCapabilityCatalog catalog;

    public FinanceiraCapabilityRbac(FinanceiraCapabilityCatalog catalog) {
        this.catalog = java.util.Objects.requireNonNull(catalog, "catalog");
    }

    private static final Set<String> BASELINE_ANY = Set.of(
            "ROLE_ADMIN",
            "ROLE_CONTADOR",
            "ROLE_FINANCEIRO",
            "ROLE_AUDITOR",
            "ROLE_ADVOGADO",
            "ROLE_SERVIDOR",
            "ROLE_SERVIDOR_FORUM"
    );

    private static final Set<String> AUDITORIA_ANY = Set.of(
            "ROLE_ADMIN",
            "ROLE_AUDITOR"
    );

    private static final Set<String> PAGAMENTO_ANY = Set.of(
            "ROLE_ADMIN",
            "ROLE_FINANCEIRO"
    );

    public boolean canExecute(Authentication authentication, IARequest request) {
        if (!isAuthenticated(authentication) || request == null) return false;

        
        if (!hasAny(authentication, BASELINE_ANY)) return false;

        String rawCap = VersionHints.resolveCapability(request.getPayload(), request.getAcao());
        String cap = CapabilityStrings.canonical(rawCap);
        if (!catalog.isAllowed(cap)) return false;

        
        if (matchesAny(cap, "AUDITORIA", "FRAUDE", "LAVAGEM", "COMPLIANCE")) {
            return hasAny(authentication, AUDITORIA_ANY);
        }

        
        if (matchesAny(cap, "PAGAMENTO", "TRANSFERENCIA", "TRANSFERÊNCIA", "PIX", "TED", "DOC")) {
            return hasAny(authentication, PAGAMENTO_ANY);
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
