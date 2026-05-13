package com.tcc.pjb.backend.legal.skills.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityCatalog;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;

@Component
@RefreshScope
public class LegalSkillsCapabilityCatalog implements CapabilityCatalog {

    private final boolean denyUnknown;
    private final boolean allowIfMissing;
    private final Set<String> exact;
    private final List<String> prefixes;
    private final Set<String> contains;

    public LegalSkillsCapabilityCatalog(LegalSkillsCapabilityCatalogProperties props) {
        Objects.requireNonNull(props, "props");
        this.denyUnknown = props.isDenyUnknown();
        this.allowIfMissing = props.isAllowIfMissing();
        this.exact = canonicalSet(props.getAllowExact());
        this.prefixes = canonicalList(props.getAllowPrefix());
        this.contains = canonicalSet(props.getAllowContainsAny());
    }

    @Override
    public boolean isAllowed(String canonicalToken) {
        if (canonicalToken == null || canonicalToken.isBlank()) {
            return allowIfMissing;
        }
        String t = canonicalToken;
        if (exact.contains(t)) return true;

        for (String p : prefixes) {
            if (p == null || p.isBlank()) continue;
            if (t.startsWith(p)) return true;
        }

        for (String token : contains) {
            if (token == null || token.isBlank()) continue;
            if (t.contains(token)) return true;
        }

        return !denyUnknown;
    }

    private static Set<String> canonicalSet(List<String> input) {
        if (input == null || input.isEmpty()) return Set.of();
        HashSet<String> out = new HashSet<>(Math.max(8, input.size() * 2));
        for (String v : input) {
            String c = CapabilityStrings.canonical(v);
            if (c != null) out.add(c);
        }
        return Set.copyOf(out);
    }

    private static List<String> canonicalList(List<String> input) {
        if (input == null || input.isEmpty()) return List.of();
        ArrayList<String> out = new ArrayList<>(input.size());
        for (String v : input) {
            String c = CapabilityStrings.canonical(v);
            if (c != null) out.add(c);
        }
        return List.copyOf(out);
    }
}
