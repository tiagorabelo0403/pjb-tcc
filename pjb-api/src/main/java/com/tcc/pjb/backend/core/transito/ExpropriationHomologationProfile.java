package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ExpropriationHomologationProfile(
        String actType,
        String assetKind,
        String homologationMode,
        String adjudicationMode,
        String arrematacaoMode,
        String titleTransferMode,
        String possessionDeliveryMode,
        String depositReleaseMode,
        String fraudReviewDesk,
        String preferenceReviewDesk,
        String queueCode,
        String inboxKey,
        TipoUsuario assignedRole,
        int priority,
        boolean blocking,
        long dueAmount,
        ChronoUnit dueUnit,
        String baseLegal,
        String settlementTriggerMode,
        String closureHint,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public ExpropriationHomologationProfile {
        assignedRole = assignedRole == null ? TipoUsuario.JUIZ : assignedRole;
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
                firstNonBlank(homologationMode, "HOMOLOGACAO"),
                firstNonBlank(assetKind, "BEM"),
                firstNonBlank(queueCode, "FILA"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("actType", actType);
        out.put("assetKind", assetKind);
        out.put("homologationMode", homologationMode);
        out.put("adjudicationMode", adjudicationMode);
        out.put("arrematacaoMode", arrematacaoMode);
        out.put("titleTransferMode", titleTransferMode);
        out.put("possessionDeliveryMode", possessionDeliveryMode);
        out.put("depositReleaseMode", depositReleaseMode);
        out.put("fraudReviewDesk", fraudReviewDesk);
        out.put("preferenceReviewDesk", preferenceReviewDesk);
        out.put("queueCode", queueCode);
        out.put("inboxKey", inboxKey);
        out.put("assignedRole", assignedRole != null ? assignedRole.name() : null);
        out.put("priority", priority);
        out.put("blocking", blocking);
        out.put("dueAmount", dueAmount);
        out.put("dueUnit", dueUnit != null ? dueUnit.name() : null);
        out.put("baseLegal", baseLegal);
        out.put("settlementTriggerMode", settlementTriggerMode);
        out.put("closureHint", closureHint);
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
