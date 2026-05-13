package com.tcc.pjb.backend.service.juiz.session;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GabineteOperationalProfile(
        String decisionDesk,
        String advisoryDesk,
        String recursalSupportDesk,
        String hearingDesk,
        String coordinationDesk,
        String sessionChannel,
        String loadBand,
        String coordinationMode,
        int urgentItems,
        int blockingItems,
        int recursalItems,
        int hearingItems,
        int secrecyItems,
        List<String> labels,
        LinkedHashMap<String, Object> metadata) {

    public GabineteOperationalProfile {
        labels = labels == null ? List.of() : List.copyOf(labels);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public String descriptor() {
        return String.join(":",
                firstNonBlank(decisionDesk, "GAB"),
                firstNonBlank(advisoryDesk, "ASSESSORIA"),
                firstNonBlank(recursalSupportDesk, "RECURSAL"),
                firstNonBlank(loadBand, "LOAD"),
                firstNonBlank(coordinationMode, "MODE"));
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("decisionDesk", decisionDesk);
        out.put("advisoryDesk", advisoryDesk);
        out.put("recursalSupportDesk", recursalSupportDesk);
        out.put("hearingDesk", hearingDesk);
        out.put("coordinationDesk", coordinationDesk);
        out.put("sessionChannel", sessionChannel);
        out.put("loadBand", loadBand);
        out.put("coordinationMode", coordinationMode);
        out.put("urgentItems", urgentItems);
        out.put("blockingItems", blockingItems);
        out.put("recursalItems", recursalItems);
        out.put("hearingItems", hearingItems);
        out.put("secrecyItems", secrecyItems);
        out.put("labels", labels);
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
