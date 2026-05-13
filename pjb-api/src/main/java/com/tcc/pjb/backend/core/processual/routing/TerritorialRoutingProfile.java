package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record TerritorialRoutingProfile(
        String mode,
        String uf,
        String cidade,
        String comarca,
        String foro,
        String secaoJudiciaria,
        String subsecaoJudiciaria,
        String circunscricao,
        String unidadeBase,
        String preventionMode,
        boolean aptoDistribuicaoAutomatica,
        List<String> warnings,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public TerritorialRoutingProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String territoryToken() {
        String normalizedUf = normalize(uf, "BR");
        String normalizedBase = normalize(firstNonBlank(subsecaoJudiciaria, comarca, cidade, foro, circunscricao), "CAPITAL");
        return normalizedUf + '_' + normalizedBase;
    }

    public String deskHint() {
        return firstNonBlank(subsecaoJudiciaria, foro, comarca, cidade, circunscricao, unidadeBase);
    }

    public String territorialLabel() {
        StringBuilder sb = new StringBuilder();
        append(sb, foro);
        append(sb, subsecaoJudiciaria);
        append(sb, comarca);
        append(sb, cidade);
        append(sb, uf);
        return sb.length() == 0 ? null : sb.toString();
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("mode", mode);
        out.put("uf", uf);
        out.put("cidade", cidade);
        out.put("comarca", comarca);
        out.put("foro", foro);
        out.put("secaoJudiciaria", secaoJudiciaria);
        out.put("subsecaoJudiciaria", subsecaoJudiciaria);
        out.put("circunscricao", circunscricao);
        out.put("unidadeBase", unidadeBase);
        out.put("preventionMode", preventionMode);
        out.put("aptoDistribuicaoAutomatica", aptoDistribuicaoAutomatica);
        out.put("warnings", warnings);
        out.put("reviewChecklist", reviewChecklist);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    private static void append(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" / ");
        }
        sb.append(value.trim());
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }
}
