package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LegalSourceAllowlist {

    private static final Set<String> ALLOWED_SOURCE_CODES = Set.of(
            "PJB",
            "PJE",
            "E-SAJ",
            "ESAJ",
            "EPROC",
            "CRETA",
            "PROJUDI",
            "SEEU",
            "CNJ",
            "DATAJUD",
            "MNI",
            "DJE",
            "STF",
            "STJ",
            "TST",
            "STM",
            "TSE"
    );

    public LegalSourceAllowlistDecision evaluate(LegalAiConversationRequest request) {
        LinkedHashSet<String> candidates = collectCandidates(request == null ? null : request.context());
        LinkedHashSet<String> allowlisted = new LinkedHashSet<>();
        LinkedHashSet<String> blocked = new LinkedHashSet<>();
        candidates.forEach(candidate -> {
            if (isAllowed(candidate)) {
                allowlisted.add(candidate);
            } else {
                blocked.add(candidate);
            }
        });
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("candidateCount", candidates.size());
        diagnostics.put("allowlistedCount", allowlisted.size());
        diagnostics.put("blockedCount", blocked.size());
        String status = candidates.isEmpty()
                ? "NO_EXTERNAL_SOURCES"
                : blocked.isEmpty() ? "ALLOWLISTED" : allowlisted.isEmpty() ? "BLOCKED" : "PARTIAL";
        return new LegalSourceAllowlistDecision(status, List.copyOf(allowlisted), List.copyOf(blocked), ImmutableViewSupport.map(diagnostics));
    }

    private LinkedHashSet<String> collectCandidates(Map<String, Object> context) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (context == null || context.isEmpty()) {
            return out;
        }
        context.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.toLowerCase(Locale.ROOT);
            if (normalizedKey.contains("source")
                    || normalizedKey.contains("origem")
                    || normalizedKey.contains("tribunal")
                    || normalizedKey.contains("authority")
                    || normalizedKey.contains("provider")
                    || normalizedKey.contains("url")
                    || normalizedKey.contains("link")
                    || normalizedKey.contains("sistema")) {
                collectValue(value, out);
            }
        });
        return out;
    }

    private void collectValue(Object value, Set<String> out) {
        if (value == null) {
            return;
        }
        if (value instanceof String text) {
            String normalized = text.trim();
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
            return;
        }
        if (value instanceof Map<?, ?> nested) {
            nested.values().forEach(item -> collectValue(item, out));
            return;
        }
        if (value instanceof List<?> list) {
            list.forEach(item -> collectValue(item, out));
            return;
        }
        out.add(String.valueOf(value));
    }

    private boolean isAllowed(String candidate) {
        String value = candidate.trim();
        if (value.isEmpty()) {
            return false;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        if (ALLOWED_SOURCE_CODES.contains(normalized)) {
            return true;
        }
        if (normalized.startsWith("PJB/") || normalized.startsWith("CNJ/")) {
            return true;
        }
        if (looksLikeUrl(value)) {
            String host = resolveHost(value);
            return host != null && (host.endsWith(".jus.br") || host.endsWith(".gov.br") || host.endsWith(".leg.br"));
        }
        return normalized.endsWith(".JUS.BR") || normalized.endsWith(".GOV.BR") || normalized.endsWith(".LEG.BR");
    }

    private boolean looksLikeUrl(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private String resolveHost(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record LegalSourceAllowlistDecision(
            String status,
            List<String> allowlistedSources,
            List<String> blockedSources,
            Map<String, Object> diagnostics
    ) {
    }
}
