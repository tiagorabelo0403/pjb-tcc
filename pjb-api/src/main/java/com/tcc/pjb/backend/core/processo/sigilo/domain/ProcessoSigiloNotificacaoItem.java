package com.tcc.pjb.backend.core.processo.sigilo.domain;

import java.util.List;

public record ProcessoSigiloNotificacaoItem(
        Long usuarioId,
        String audienceCode,
        String audienceLabel,
        List<String> channels,
        boolean highPriority,
        String title,
        String message,
        String action,
        String deepLink,
        String rationale
) {
    public ProcessoSigiloNotificacaoItem {
        audienceCode = normalize(audienceCode, "audienceCode");
        audienceLabel = normalize(audienceLabel, "audienceLabel");
        channels = channels == null ? List.of() : List.copyOf(channels);
        title = normalize(title, "title");
        message = normalize(message, "message");
        action = normalize(action, "action");
        deepLink = normalizeOptional(deepLink);
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
