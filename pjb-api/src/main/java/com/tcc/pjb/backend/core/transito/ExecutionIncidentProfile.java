package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ExecutionIncidentProfile(
        String incidentType,
        String incidentMode,
        String admissibilityTrack,
        String queueCode,
        String inboxKey,
        TipoUsuario assignedRole,
        int priority,
        boolean blocking,
        long dueAmount,
        ChronoUnit dueUnit,
        String baseLegal,
        String contradictionMode,
        String evidenceMode,
        String escalationDesk,
        String executionImpact,
        String preventionMode,
        List<String> labels,
        List<String> warnings,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public ExecutionIncidentProfile {
        assignedRole = assignedRole == null ? TipoUsuario.SERVIDOR_FORUM : assignedRole;
        priority = Math.max(priority, 0);
        dueAmount = Math.max(dueAmount, 0L);
        dueUnit = dueUnit == null ? ChronoUnit.DAYS : dueUnit;
        labels = labels == null ? List.of() : List.copyOf(labels);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public Instant dueAtFrom(Instant base) {
        Instant anchor = base == null ? Instant.now() : base;
        return dueAmount <= 0L ? anchor : anchor.plus(dueAmount, dueUnit);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(incidentType, "INCIDENTE"),
                firstNonBlank(incidentMode, "MODO"),
                firstNonBlank(admissibilityTrack, "ADMISSIBILIDADE"),
                firstNonBlank(queueCode, "FILA"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("incidentType", incidentType);
        out.put("incidentMode", incidentMode);
        out.put("admissibilityTrack", admissibilityTrack);
        out.put("queueCode", queueCode);
        out.put("inboxKey", inboxKey);
        out.put("assignedRole", assignedRole != null ? assignedRole.name() : null);
        out.put("priority", priority);
        out.put("blocking", blocking);
        out.put("dueAmount", dueAmount);
        out.put("dueUnit", dueUnit != null ? dueUnit.name() : null);
        out.put("baseLegal", baseLegal);
        out.put("contradictionMode", contradictionMode);
        out.put("evidenceMode", evidenceMode);
        out.put("escalationDesk", escalationDesk);
        out.put("executionImpact", executionImpact);
        out.put("preventionMode", preventionMode);
        out.put("descriptor", descriptor());
        out.put("labels", labels);
        out.put("warnings", warnings);
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


    public java.util.List<String> fundamentosMateriais() {
        if (baseLegal == null || baseLegal.isBlank()) {
            return java.util.List.of();
        }
        return java.util.List.of(baseLegal.trim());
    }

    public java.util.List<String> fundamentos() {
        return fundamentosMateriais();
    }
}
