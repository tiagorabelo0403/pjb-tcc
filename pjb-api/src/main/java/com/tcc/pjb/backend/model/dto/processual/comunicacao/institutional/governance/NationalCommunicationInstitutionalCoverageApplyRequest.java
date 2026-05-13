package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import jakarta.validation.constraints.NotBlank;

public record NationalCommunicationInstitutionalCoverageApplyRequest(
        @NotBlank String expedicaoUuid,
        String motivoComplementar
) {
}
