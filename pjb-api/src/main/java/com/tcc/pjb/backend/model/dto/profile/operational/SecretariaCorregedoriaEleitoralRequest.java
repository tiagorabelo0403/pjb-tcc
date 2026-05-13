package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaCorregedoriaEleitoralRequest(
        @NotBlank String tipoProcedimento,
        @NotBlank String fundamento,
        String corregedorResponsavel,
        String unidadeAlvo,
        Boolean urgente,
        String observacao
) {
    public String tipoProcedimentoResolvido() {
        return normalize(tipoProcedimento, "PROCEDIMENTO_CORREICIONAL");
    }

    public String fundamentoResolvido() {
        return normalize(fundamento, "FUNDAMENTO_NAO_INFORMADO");
    }

    public String corregedorResponsavelResolvido() {
        return normalize(corregedorResponsavel, "CORREGEDORIA_ELEITORAL_PJB");
    }

    public String unidadeAlvoResolvida() {
        return normalize(unidadeAlvo, "UNIDADE_NAO_INFORMADA");
    }

    public boolean urgenteResolvido() {
        return Boolean.TRUE.equals(urgente);
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
