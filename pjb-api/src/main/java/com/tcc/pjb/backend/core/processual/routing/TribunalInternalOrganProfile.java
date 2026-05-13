package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TribunalInternalOrganProfile(
        String macroOrgan,
        String specificOrgan,
        String competenceCluster,
        String secretariatDesk,
        String gabineteDesk,
        String sessionChannel,
        String admissibilityPath,
        String preventionBucket,
        String quorumHint,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public TribunalInternalOrganProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveSpecificOrgan(String fallback) {
        return firstNonBlank(specificOrgan, fallback);
    }

    public String effectiveSecretariatDesk(String fallback) {
        return firstNonBlank(secretariatDesk, fallback);
    }

    public String effectiveGabineteDesk(String fallback) {
        return firstNonBlank(gabineteDesk, fallback);
    }

    public String effectiveSessionChannel(String fallback) {
        return firstNonBlank(sessionChannel, fallback);
    }

    public String effectiveAdmissibilityPath(String fallback) {
        return firstNonBlank(admissibilityPath, fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("macroOrgan", macroOrgan);
        out.put("specificOrgan", specificOrgan);
        out.put("competenceCluster", competenceCluster);
        out.put("secretariatDesk", secretariatDesk);
        out.put("gabineteDesk", gabineteDesk);
        out.put("sessionChannel", sessionChannel);
        out.put("admissibilityPath", admissibilityPath);
        out.put("preventionBucket", preventionBucket);
        out.put("quorumHint", quorumHint);
        out.put("warnings", warnings);
        out.put("fundamentos", fundamentos);
        out.put("reviewChecklist", reviewChecklist);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
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
}
