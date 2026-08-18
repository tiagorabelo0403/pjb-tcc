package com.tcc.pjb.backend.model.dto.cidadao;

import jakarta.validation.constraints.NotBlank;

public record CidadaoSolicitacaoAjgRequest(
        @NotBlank String renda,
        @NotBlank String justificativa
) {
}
