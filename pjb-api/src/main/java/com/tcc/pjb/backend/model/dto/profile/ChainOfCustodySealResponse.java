package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record ChainOfCustodySealResponse(
        String loteReferencia,
        String digestColecaoSha256,
        String chaveCustodia,
        String sealedBy,
        Instant sealedAt,
        List<SealedEvidence> evidencias
) {

    public record SealedEvidence(
            String id,
            String nome,
            String digestSha256,
            String chaveCustodia,
            Instant sealedAt,
            List<String> metadadosCanonicos
    ) {
    }
}
