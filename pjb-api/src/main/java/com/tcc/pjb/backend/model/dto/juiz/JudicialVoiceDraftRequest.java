package com.tcc.pjb.backend.model.dto.juiz;

import jakarta.validation.constraints.NotNull;

public record JudicialVoiceDraftRequest(
        @NotNull Long processoId,
        String modoDocumento,
        String transcricaoBruta
) {
}
