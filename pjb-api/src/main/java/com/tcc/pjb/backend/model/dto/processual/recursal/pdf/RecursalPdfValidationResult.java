package com.tcc.pjb.backend.model.dto.processual.recursal.pdf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecursalPdfValidationResult(
        boolean valid,
        String status,
        List<String> errors,
        List<String> warnings,
        Map<String, Object> details) {

    public RecursalPdfValidationResult {
        status = normalize(status);
        errors = errors == null || errors.isEmpty() ? List.of() : List.copyOf(errors);
        warnings = warnings == null || warnings.isEmpty() ? List.of() : List.copyOf(warnings);
        details = sanitizeMap(details);
    }

    public static RecursalPdfValidationResult invalid(String status, List<String> errors, List<String> warnings, Map<String, Object> details) {
        return new RecursalPdfValidationResult(false, status, errors, warnings, details);
    }

    public static RecursalPdfValidationResult valid(String status, List<String> warnings, Map<String, Object> details) {
        return new RecursalPdfValidationResult(true, status, List.of(), warnings, details);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("valid", valid);
        out.put("status", status);
        if (!errors.isEmpty()) {
            out.put("errors", errors);
        }
        if (!warnings.isEmpty()) {
            out.put("warnings", warnings);
        }
        if (!details.isEmpty()) {
            out.put("details", details);
        }
        return Map.copyOf(out);
    }

    private static Map<String, Object> sanitizeMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        value.forEach((key, item) -> {
            if (key != null && item != null) {
                sanitized.put(key, item);
            }
        });
        return sanitized.isEmpty() ? Map.of() : Map.copyOf(sanitized);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
