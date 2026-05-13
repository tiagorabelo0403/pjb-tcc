package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import jakarta.validation.constraints.NotBlank;

public record NationalCommunicationInstitutionalRedistributeRequest(
        @NotBlank String expedicaoUuid,
        @NotBlank String caixaDestinoCodigo,
        String detalhe
) {
}
