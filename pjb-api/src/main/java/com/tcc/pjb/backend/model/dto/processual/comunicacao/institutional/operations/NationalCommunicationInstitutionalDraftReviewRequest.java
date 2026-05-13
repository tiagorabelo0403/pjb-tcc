package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import jakarta.validation.constraints.NotBlank;

public record NationalCommunicationInstitutionalDraftReviewRequest(
        @NotBlank String expedicaoUuid,
        @NotBlank String draftId,
        String observacoes,
        Boolean autoCumprir
) {
}
