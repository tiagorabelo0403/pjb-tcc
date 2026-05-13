package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ExternalConstrictionReconciliationProfile(
        String actType,
        String assetKind,
        String gatewayCode,
        String externalStatus,
        String reconciliationStatus,
        String contingencyDesk,
        String reconciliationDesk,
        String retryWindowMode,
        String proofDesk,
        String finalizationMode,
        String queueCode,
        String inboxKey,
        TipoUsuario assignedRole,
        int priority,
        boolean blocking,
        long dueAmount,
        ChronoUnit dueUnit,
        String baseLegal,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public ExternalConstrictionReconciliationProfile {
        assignedRole = assignedRole == null ? TipoUsuario.SERVIDOR_FORUM : assignedRole;
        priority = Math.max(priority, 0);
        dueAmount = Math.max(dueAmount, 0L);
        dueUnit = dueUnit == null ? ChronoUnit.HOURS : dueUnit;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public Instant dueAtFrom(Instant base) {
        Instant anchor = base == null ? Instant.now() : base;
        return dueAmount <= 0L ? anchor : anchor.plus(dueAmount, dueUnit);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(gatewayCode, "GATEWAY"),
                firstNonBlank(externalStatus, "STATUS_EXTERNO"),
                firstNonBlank(reconciliationStatus, "RECONCILIACAO"),
                firstNonBlank(queueCode, "FILA"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("actType", actType);
        out.put("assetKind", assetKind);
        out.put("gatewayCode", gatewayCode);
        out.put("externalStatus", externalStatus);
        out.put("reconciliationStatus", reconciliationStatus);
        out.put("contingencyDesk", contingencyDesk);
        out.put("reconciliationDesk", reconciliationDesk);
        out.put("retryWindowMode", retryWindowMode);
        out.put("proofDesk", proofDesk);
        out.put("finalizationMode", finalizationMode);
        out.put("queueCode", queueCode);
        out.put("inboxKey", inboxKey);
        out.put("assignedRole", assignedRole != null ? assignedRole.name() : null);
        out.put("priority", priority);
        out.put("blocking", blocking);
        out.put("dueAmount", dueAmount);
        out.put("dueUnit", dueUnit != null ? dueUnit.name() : null);
        out.put("baseLegal", baseLegal);
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
