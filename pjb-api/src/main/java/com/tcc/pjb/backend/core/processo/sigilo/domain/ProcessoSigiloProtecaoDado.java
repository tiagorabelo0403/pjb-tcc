package com.tcc.pjb.backend.core.processo.sigilo.domain;


public record ProcessoSigiloProtecaoDado(
        String field,
        String classification,
        String action,
        String maskedValue,
        boolean required,
        String rationale
) {
    public ProcessoSigiloProtecaoDado {
        field = normalize(field, "field");
        classification = normalize(classification, "classification");
        action = normalize(action, "action");
        maskedValue = normalizeOptional(maskedValue);
        rationale = normalize(rationale, "rationale");
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
