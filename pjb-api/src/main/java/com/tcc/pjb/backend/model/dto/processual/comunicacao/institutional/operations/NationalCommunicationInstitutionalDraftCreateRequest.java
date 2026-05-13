package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import jakarta.validation.constraints.NotBlank;

public record NationalCommunicationInstitutionalDraftCreateRequest(
        @NotBlank String expedicaoUuid,
        @NotBlank String titulo,
        @NotBlank String conteudo,
        String observacoes
) {
}
