package com.tcc.pjb.backend.model.dto.profile;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ChainOfCustodySealRequest(
        @Valid @NotEmpty List<EvidenceItem> evidencias,
        String loteReferencia
) {

    public record EvidenceItem(
            String id,
            String nome,
            String conteudoBase64,
            String digestSha256Externo,
            Map<String, String> metadados
    ) {
    }
}
