package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaPesquisaEleitoralRequest(
        @NotBlank String instituto,
        @NotBlank String registroPesquisa,
        String periodoDivulgacao,
        String midiaReferencia,
        Boolean deferir,
        String observacao
) {
    public String institutoResolvido() {
        return normalize(instituto, "INSTITUTO_NAO_INFORMADO");
    }

    public String registroPesquisaResolvido() {
        return normalize(registroPesquisa, "REGISTRO_NAO_INFORMADO");
    }

    public String periodoDivulgacaoResolvido() {
        return normalize(periodoDivulgacao, "PERIODO_NAO_INFORMADO");
    }

    public String midiaReferenciaResolvida() {
        return normalize(midiaReferencia, "MIDIA_NAO_INFORMADA");
    }

    public boolean deferirResolvido() {
        return deferir == null || deferir;
    }

    public String observacaoResolvida() {
        return normalize(observacao, null);
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
