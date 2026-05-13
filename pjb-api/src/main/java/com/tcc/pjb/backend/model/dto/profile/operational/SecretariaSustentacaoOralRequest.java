package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SecretariaSustentacaoOralRequest(
        @NotBlank String solicitanteNome,
        String documentoRepresentacao,
        String midiaReferencia,
        Boolean remota,
        @Min(1) @Max(180) Integer duracaoMinutos,
        String observacao
) {
    public boolean remotaResolvida() {
        return Boolean.TRUE.equals(remota);
    }

    public int duracaoMinutosResolvida() {
        return duracaoMinutos == null || duracaoMinutos < 1 ? 15 : duracaoMinutos;
    }

    public String solicitanteNomeResolvido() {
        return solicitanteNome == null ? null : solicitanteNome.trim();
    }

    public String documentoRepresentacaoResolvido() {
        return normalize(documentoRepresentacao);
    }

    public String midiaReferenciaResolvida() {
        return normalize(midiaReferencia);
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
