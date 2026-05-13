package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaInspecaoCorregedoriaRequest(
        @NotBlank String cicloInspecao,
        @NotBlank String unidadeInspecionada,
        String relatorioReferencia,
        Boolean irregularidadeCritica,
        String observacao
) {
    public String cicloInspecaoResolvido() {
        return normalize(cicloInspecao, "CICLO_NAO_INFORMADO");
    }

    public String unidadeInspecionadaResolvida() {
        return normalize(unidadeInspecionada, "UNIDADE_NAO_INFORMADA");
    }

    public String relatorioReferenciaResolvida() {
        return normalize(relatorioReferencia, "RELATORIO_PENDENTE");
    }

    public boolean irregularidadeCriticaResolvida() {
        return Boolean.TRUE.equals(irregularidadeCritica);
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
