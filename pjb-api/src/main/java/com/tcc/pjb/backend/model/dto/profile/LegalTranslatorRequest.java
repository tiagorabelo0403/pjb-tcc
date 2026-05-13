package com.tcc.pjb.backend.model.dto.profile;

import jakarta.validation.constraints.Size;

public record LegalTranslatorRequest(
        Long processoId,
        @Size(max = 50) String numeroProcesso,
        @Size(max = 12000) String textoLivre,
        boolean incluirProximosPassos,
        boolean incluirGlossario
) {
}
