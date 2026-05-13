package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NationalCommunicationInstitutionalDraftSubmitRequest(
        @NotBlank String expedicaoUuid,
        @NotBlank String draftId,
        @NotNull Long aprovadorUsuarioId,
        String observacoes
) {
}
