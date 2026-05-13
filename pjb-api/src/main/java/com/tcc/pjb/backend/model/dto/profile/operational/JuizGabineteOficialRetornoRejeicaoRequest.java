package com.tcc.pjb.backend.model.dto.profile.operational;

public record JuizGabineteOficialRetornoRejeicaoRequest(
        String fundamentoRejeicao,
        String observacao,
        Boolean concluirItemOrigem
) {

    public String fundamentoRejeicaoResolvido(String fallback) {
        return normalize(fundamentoRejeicao, fallback);
    }

    public String observacaoResolvida() {
        return normalize(observacao, null);
    }

    public boolean concluirItemOrigemResolvido(boolean fallback) {
        return concluirItemOrigem != null ? concluirItemOrigem : fallback;
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
