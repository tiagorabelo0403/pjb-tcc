package com.tcc.pjb.backend.model.dto.juiz;

import jakarta.validation.constraints.NotBlank;

public record JudicialVoiceSessionChunkRequest(
        @NotBlank String trecho,
        boolean parcial
) {
}
