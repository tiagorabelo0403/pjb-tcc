package com.tcc.pjb.backend.model.dto.ui.frontend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FrontendOfficeGovernedPetitionRequest(
        @NotBlank @Size(max = 120) String tipoPeticao,
        @NotBlank @Size(max = 200000) String conteudo,
        @Size(max = 10000) String fundamentacao
) {
}
