package com.tcc.pjb.backend.model.dto.ai.legal.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LegalAiJuridicalLineageDescriptor(
        String branchCode,
        String branchName,
        String parentTradition,
        List<String> foundationalReferences,
        List<String> brazilianReferences,
        List<String> hermeneuticLenses,
        List<String> authorityChecks
) {
    public LegalAiJuridicalLineageDescriptor {
        branchCode = safe(branchCode);
        branchName = safe(branchName);
        parentTradition = safe(parentTradition);
        foundationalReferences = safeList(foundationalReferences);
        brazilianReferences = safeList(brazilianReferences);
        hermeneuticLenses = safeList(hermeneuticLenses);
        authorityChecks = safeList(authorityChecks);
    }

    public Map<String, Object> asMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("branchCode", branchCode);
        out.put("branchName", branchName);
        out.put("parentTradition", parentTradition);
        out.put("foundationalReferences", foundationalReferences);
        out.put("brazilianReferences", brazilianReferences);
        out.put("hermeneuticLenses", hermeneuticLenses);
        out.put("authorityChecks", authorityChecks);
        return Collections.unmodifiableMap(out);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> safeList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
