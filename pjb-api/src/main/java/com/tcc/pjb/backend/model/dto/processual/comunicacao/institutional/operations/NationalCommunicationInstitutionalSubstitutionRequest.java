package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NationalCommunicationInstitutionalSubstitutionRequest(
        @NotBlank String expedicaoUuid,
        @NotNull Long substitutoUsuarioId,
        Integer horasVigencia,
        String motivo
) {
}
