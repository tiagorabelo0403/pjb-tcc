package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaIntimacaoRequest(
        @NotBlank String destinatario,
        @NotBlank String conteudo,
        @NotBlank String prazo,
        Long oficialId,
        Boolean reativarOficial,
        String origemOperacional,
        String fundamentoOperacional,
        String observacaoOperacional,
        Boolean manterRetornoForumAberto
) {
    public boolean reativarOficialResolvido() {
        if (Boolean.TRUE.equals(reativarOficial)) {
            return true;
        }
        if (oficialId != null) {
            return true;
        }
        if (destinatario == null || destinatario.isBlank()) {
            return false;
        }
        String normalized = destinatario.trim().toUpperCase();
        return normalized.contains("OFICIAL") || normalized.contains("MANDADO") || normalized.contains("CUMPRIMENTO");
    }

    public String origemOperacionalResolvida() {
        if (origemOperacional == null || origemOperacional.isBlank()) {
            return "SECRETARIA";
        }
        return origemOperacional.trim().toUpperCase();
    }

    public String fundamentoOperacionalResolvido() {
        return normalize(fundamentoOperacional);
    }

    public String observacaoOperacionalResolvida() {
        return normalize(observacaoOperacional);
    }

    public boolean manterRetornoForumAbertoResolvido() {
        return Boolean.TRUE.equals(manterRetornoForumAberto);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
