package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ExpropriationGovernanceProfile(
        String actType,
        String assetKind,
        String expropriationMode,
        String sessionMode,
        String publicationMode,
        String depositaryMode,
        String leiloeiroMode,
        String priceFloorMode,
        String preferenceDesk,
        String fraudReviewDesk,
        String queueCode,
        String inboxKey,
        TipoUsuario assignedRole,
        int priority,
        boolean blocking,
        long dueAmount,
        ChronoUnit dueUnit,
        String baseLegal,
        String settlementMode,
        String deliveryMode,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public ExpropriationGovernanceProfile {
        assignedRole = assignedRole == null ? TipoUsuario.LEILOEIRO_JUDICIAL : assignedRole;
        priority = Math.max(priority, 0);
        dueAmount = Math.max(dueAmount, 0L);
        dueUnit = dueUnit == null ? ChronoUnit.DAYS : dueUnit;
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
                firstNonBlank(actType, "ATO"),
                firstNonBlank(expropriationMode, "MODO"),
                firstNonBlank(assetKind, "BEM"),
                firstNonBlank(queueCode, "FILA"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("actType", actType);
        out.put("assetKind", assetKind);
        out.put("expropriationMode", expropriationMode);
        out.put("sessionMode", sessionMode);
        out.put("publicationMode", publicationMode);
        out.put("depositaryMode", depositaryMode);
        out.put("leiloeiroMode", leiloeiroMode);
        out.put("priceFloorMode", priceFloorMode);
        out.put("preferenceDesk", preferenceDesk);
        out.put("fraudReviewDesk", fraudReviewDesk);
        out.put("queueCode", queueCode);
        out.put("inboxKey", inboxKey);
        out.put("assignedRole", assignedRole != null ? assignedRole.name() : null);
        out.put("priority", priority);
        out.put("blocking", blocking);
        out.put("dueAmount", dueAmount);
        out.put("dueUnit", dueUnit != null ? dueUnit.name() : null);
        out.put("baseLegal", baseLegal);
        out.put("settlementMode", settlementMode);
        out.put("deliveryMode", deliveryMode);
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
