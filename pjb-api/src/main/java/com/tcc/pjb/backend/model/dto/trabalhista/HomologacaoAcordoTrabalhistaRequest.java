package com.tcc.pjb.backend.model.dto.trabalhista;

import jakarta.validation.constraints.NotBlank;

public record HomologacaoAcordoTrabalhistaRequest(
        @NotBlank String resumo
) {
}
