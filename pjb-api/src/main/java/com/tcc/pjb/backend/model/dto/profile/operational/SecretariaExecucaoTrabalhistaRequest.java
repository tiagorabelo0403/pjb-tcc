package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaExecucaoTrabalhistaRequest(
        @NotBlank String medidaExecutiva,
        String gruReferencia,
        String depositoJudicialReferencia,
        Boolean urgente,
        String observacao
) {
    public String medidaExecutivaResolvida() {
        return normalize(medidaExecutiva, "MEDIDA_EXECUTIVA_NAO_INFORMADA");
    }

    public String gruReferenciaResolvida() {
        return normalize(gruReferencia, "GRU_PENDENTE");
    }

    public String depositoJudicialReferenciaResolvida() {
        return normalize(depositoJudicialReferencia, "DEPOSITO_NAO_INFORMADO");
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
