package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaMidiaProcessualRequest(
        @NotBlank String tipoMidia,
        @NotBlank String referenciaArquivo,
        String origemMidia,
        String descricao,
        Boolean sigilosa,
        Boolean disponibilizarParaSessao
) {
    public String tipoMidiaResolvido() {
        return normalize(tipoMidia, "ARQUIVO_PROCESSUAL");
    }

    public String referenciaArquivoResolvida() {
        return normalize(referenciaArquivo, "REFERENCIA_NAO_INFORMADA");
    }

    public String origemMidiaResolvida() {
        return normalize(origemMidia, "ACERVO_DIGITAL_PJB");
    }

    public String descricaoResolvida() {
        return normalize(descricao, null);
    }

    public boolean sigilosaResolvida() {
        return Boolean.TRUE.equals(sigilosa);
    }

    public boolean disponibilizarParaSessaoResolvida() {
        return Boolean.TRUE.equals(disponibilizarParaSessao);
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
