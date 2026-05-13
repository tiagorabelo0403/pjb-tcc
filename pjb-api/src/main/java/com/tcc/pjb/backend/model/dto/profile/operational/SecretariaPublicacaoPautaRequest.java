package com.tcc.pjb.backend.model.dto.profile.operational;

import java.time.LocalDateTime;

public record SecretariaPublicacaoPautaRequest(
        LocalDateTime pautaDataHora,
        String editalReferencia,
        String canalPublicacao,
        String observacao
) {
    public String editalReferenciaResolvida() {
        return normalize(editalReferencia);
    }

    public String canalPublicacaoResolvido() {
        return normalize(canalPublicacao);
    }

    public String observacaoResolvida() {
        return normalize(observacao);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
