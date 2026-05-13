package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import jakarta.validation.constraints.NotBlank;

public record NationalCommunicationInstitutionalScienceRequest(
        @NotBlank String expedicaoUuid,
        String detalhe
) {
}
