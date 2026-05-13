package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record NationalCommunicationInstitutionalBulkRequest(
        @NotEmpty List<String> expedicoesUuids,
        String detalhe
) {
}
