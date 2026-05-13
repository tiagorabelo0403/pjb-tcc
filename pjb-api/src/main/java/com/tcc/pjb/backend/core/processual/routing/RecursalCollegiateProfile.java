package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecursalCollegiateProfile(
        String tribunalDetalhadoCodigo,
        String tribunalDetalhadoNome,
        String colegiadoNatural,
        String authorityMode,
        String admissibilityAuthority,
        String specialReviewAuthority,
        String presidencyDesk,
        String uniformizationHub,
        String cluster,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public RecursalCollegiateProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("tribunalDetalhadoCodigo", tribunalDetalhadoCodigo);
        out.put("tribunalDetalhadoNome", tribunalDetalhadoNome);
        out.put("colegiadoNatural", colegiadoNatural);
        out.put("authorityMode", authorityMode);
        out.put("admissibilityAuthority", admissibilityAuthority);
        out.put("specialReviewAuthority", specialReviewAuthority);
        out.put("presidencyDesk", presidencyDesk);
        out.put("uniformizationHub", uniformizationHub);
        out.put("cluster", cluster);
        out.put("warnings", warnings);
        out.put("fundamentos", fundamentos);
        out.put("reviewChecklist", reviewChecklist);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
