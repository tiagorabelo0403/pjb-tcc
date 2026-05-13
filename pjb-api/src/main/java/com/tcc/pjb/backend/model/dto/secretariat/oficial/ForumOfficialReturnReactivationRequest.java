package com.tcc.pjb.backend.model.dto.secretariat.oficial;

import java.time.Instant;

public record ForumOfficialReturnReactivationRequest(
        Long oficialId,
        String origemReativacao,
        String fundamento,
        String observacao,
        Instant dueAt,
        Boolean manterDeskAberto
) {
    public String origemReativacaoResolvida() {
        if (origemReativacao == null || origemReativacao.isBlank()) {
            return "SECRETARIA";
        }
        return origemReativacao.trim().toUpperCase();
    }

    public String fundamentoResolvido() {
        return normalize(fundamento);
    }

    public String observacaoResolvida() {
        return normalize(observacao);
    }

    public boolean manterDeskAbertoResolvido() {
        return Boolean.TRUE.equals(manterDeskAberto);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
