package com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao;

public record RecursalFormalizacaoTextos(
        String razoes,
        String fundamentacao,
        String observacoes) {

    public RecursalFormalizacaoTextos {
        razoes = normalize(razoes);
        fundamentacao = normalize(fundamentacao);
        observacoes = normalize(observacoes);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
