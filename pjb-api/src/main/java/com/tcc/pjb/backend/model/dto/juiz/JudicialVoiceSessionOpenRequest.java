package com.tcc.pjb.backend.model.dto.juiz;

import jakarta.validation.constraints.NotNull;

public record JudicialVoiceSessionOpenRequest(
        @NotNull Long processoId,
        String modoDocumento,
        String primeiraCaptura
) {
}
