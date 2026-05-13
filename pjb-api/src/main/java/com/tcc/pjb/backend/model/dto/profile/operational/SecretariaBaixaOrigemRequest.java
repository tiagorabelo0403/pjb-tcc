package com.tcc.pjb.backend.model.dto.profile.operational;

public record SecretariaBaixaOrigemRequest(
        String destinoOrigem,
        String fundamento,
        Boolean arquivarAposBaixa
) {
    public String destinoOrigemResolvido() {
        return normalize(destinoOrigem);
    }

    public String fundamentoResolvido() {
        return normalize(fundamento);
    }

    public boolean arquivarAposBaixaResolvida() {
        return Boolean.TRUE.equals(arquivarAposBaixa);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
