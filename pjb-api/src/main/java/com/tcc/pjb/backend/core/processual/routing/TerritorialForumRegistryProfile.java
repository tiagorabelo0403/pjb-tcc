package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TerritorialForumRegistryProfile(
        String municipalAnchor,
        String judicialDistrictLabel,
        String primaryForum,
        String secondaryForum,
        String supportDesk,
        String venueClass,
        String distributionCluster,
        List<String> warnings,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata) {

    public TerritorialForumRegistryProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String effectiveMunicipalAnchor(String fallback) {
        return firstNonBlank(municipalAnchor, fallback);
    }

    public String effectiveJudicialDistrict(String fallback) {
        return firstNonBlank(judicialDistrictLabel, fallback);
    }

    public String effectivePrimaryForum(String fallback) {
        return firstNonBlank(primaryForum, fallback);
    }

    public String effectiveSecondaryForum(String fallback) {
        return firstNonBlank(secondaryForum, fallback);
    }

    public String effectiveSupportDesk(String fallback) {
        return firstNonBlank(supportDesk, fallback);
    }

    public String effectiveDistributionCluster(String fallback) {
        return firstNonBlank(distributionCluster, fallback);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("municipalAnchor", municipalAnchor);
        out.put("judicialDistrictLabel", judicialDistrictLabel);
        out.put("primaryForum", primaryForum);
        out.put("secondaryForum", secondaryForum);
        out.put("supportDesk", supportDesk);
        out.put("venueClass", venueClass);
        out.put("distributionCluster", distributionCluster);
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
