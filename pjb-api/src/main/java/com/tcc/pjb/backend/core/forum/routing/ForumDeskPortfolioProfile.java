package com.tcc.pjb.backend.core.forum.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.core.util.PayloadMaps;

public record ForumDeskPortfolioProfile(
        String triageDesk,
        String gabineteDesk,
        String hearingDesk,
        String complianceDesk,
        String escalationDesk,
        String assistantDesk,
        String coordinationDesk,
        String redistributionDesk,
        String dashboardBucket,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public ForumDeskPortfolioProfile {
        labels = PayloadMaps.copyTrimmedStrings(labels);
        metadata = new LinkedHashMap<>(PayloadMaps.deepCopyWithoutNulls(metadata));
    }

    public String operationalDescriptor() {
        return String.join(":",
                firstNonBlank(triageDesk, "TRIAGE"),
                firstNonBlank(gabineteDesk, "GABINETE"),
                firstNonBlank(complianceDesk, "COMPLIANCE"),
                firstNonBlank(redistributionDesk, "REDIST"),
                firstNonBlank(dashboardBucket, "DASH"));
    }

    public String coordinationDescriptor() {
        return String.join(":",
                firstNonBlank(coordinationDesk, "COORD"),
                firstNonBlank(assistantDesk, "ASSIST"),
                firstNonBlank(escalationDesk, "ESC"));
    }

    public String executionDesk() {
        return firstNonBlank(complianceDesk, hearingDesk, redistributionDesk, coordinationDesk);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("triageDesk", triageDesk);
        out.put("gabineteDesk", gabineteDesk);
        out.put("hearingDesk", hearingDesk);
        out.put("complianceDesk", complianceDesk);
        out.put("escalationDesk", escalationDesk);
        out.put("assistantDesk", assistantDesk);
        out.put("coordinationDesk", coordinationDesk);
        out.put("redistributionDesk", redistributionDesk);
        out.put("dashboardBucket", dashboardBucket);
        out.put("labels", labels);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    public static ForumDeskPortfolioProfile defaultProfile(String dashboardBucket) {
        return new ForumDeskPortfolioProfile("TRIAGE", "GABINETE", "HEARING", "COMPLIANCE", "ESCALATION", "ASSISTANT", "COORDINATION", "REDISTRIBUTION", firstNonBlank(dashboardBucket, "TRIAGE"), java.util.List.of(), new java.util.LinkedHashMap<>());
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
