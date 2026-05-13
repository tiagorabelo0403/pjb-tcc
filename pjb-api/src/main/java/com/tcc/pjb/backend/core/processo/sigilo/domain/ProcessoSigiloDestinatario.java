package com.tcc.pjb.backend.core.processo.sigilo.domain;

import java.util.List;

public record ProcessoSigiloDestinatario(
        Long usuarioId,
        String audienceCode,
        String audienceLabel,
        String tipoUsuario,
        String nome,
        List<String> channels,
        boolean onlyAfterJudicialDecree,
        boolean exigeStepUp,
        boolean exigeCredencial,
        String rationale
) {
    public ProcessoSigiloDestinatario {
        audienceCode = normalize(audienceCode, "audienceCode");
        audienceLabel = normalize(audienceLabel, "audienceLabel");
        tipoUsuario = normalizeOptional(tipoUsuario);
        nome = normalizeOptional(nome);
        channels = channels == null ? List.of() : List.copyOf(channels);
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
