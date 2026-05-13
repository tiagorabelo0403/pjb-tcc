package com.tcc.pjb.backend.model.dto.processual.comunicacao.flow;

import jakarta.validation.constraints.NotBlank;

public record NationalCommunicationFallbackRequest(
        @NotBlank String expedicaoUuid,
        @NotBlank String motivoFrustracao) {
}
