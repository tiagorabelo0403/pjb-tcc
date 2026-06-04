package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DelegadoDiligenciaRequest(
        @NotNull @Positive Long processoId,
        @NotNull @Positive Long inqueritoId,
        @NotNull @Positive Long unidadeApuracaoId,
        @NotBlank @Size(max = 4000) String descricao,
        @Size(max = 2000) String fundamentoOperacional,
        @Size(max = 40) String prioridade
) {
    public DelegadoDiligenciaRequest {
        descricao = trim(descricao);
        fundamentoOperacional = trim(fundamentoOperacional);
        prioridade = trim(prioridade);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
