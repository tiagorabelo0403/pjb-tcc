package com.tcc.pjb.backend.core.distribuicao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DistributionGovernanceProfile(
        String governanceMode,
        String queueLane,
        String workloadClass,
        String randomizationMode,
        String equalizationRule,
        String preventionLockMode,
        String urgencyLane,
        String sigiloLane,
        String recusalLane,
        String auditDesk,
        String incidentDesk,
        int priorityFloor,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public DistributionGovernanceProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(governanceMode, "GOVERNANCA"),
                firstNonBlank(randomizationMode, "RANDOMIZACAO"),
                firstNonBlank(preventionLockMode, "PREVENCAO"),
                firstNonBlank(urgencyLane, "ROTINA"),
                firstNonBlank(sigiloLane, "PUBLICIDADE"));
    }

    public int effectivePriority(int base) {
        return Math.min(Math.max(base, 0), Math.max(priorityFloor, 0));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("governanceMode", governanceMode);
        out.put("queueLane", queueLane);
        out.put("workloadClass", workloadClass);
        out.put("randomizationMode", randomizationMode);
        out.put("equalizationRule", equalizationRule);
        out.put("preventionLockMode", preventionLockMode);
        out.put("urgencyLane", urgencyLane);
        out.put("sigiloLane", sigiloLane);
        out.put("recusalLane", recusalLane);
        out.put("auditDesk", auditDesk);
        out.put("incidentDesk", incidentDesk);
        out.put("priorityFloor", priorityFloor);
        out.put("descriptor", descriptor());
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
