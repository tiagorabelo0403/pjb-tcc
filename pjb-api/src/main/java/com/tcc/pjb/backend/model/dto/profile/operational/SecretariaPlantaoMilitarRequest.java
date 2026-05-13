package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaPlantaoMilitarRequest(
        @NotBlank String classificacaoUrgencia,
        @NotBlank String fundamentoUrgencia,
        String autoridadePlantao,
        String canalAtendimento,
        String observacao
) {
    public String classificacaoUrgenciaResolvida() {
        return normalize(classificacaoUrgencia, "URGENTE");
    }

    public String fundamentoUrgenciaResolvido() {
        return normalize(fundamentoUrgencia, "FUNDAMENTO_NAO_INFORMADO");
    }

    public String autoridadePlantaoResolvida() {
        return normalize(autoridadePlantao, "AUTORIDADE_DE_PLANTAO_PJB");
    }

    public String canalAtendimentoResolvido() {
        return normalize(canalAtendimento, "PLANTAO_VIRTUAL_PJB");
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
