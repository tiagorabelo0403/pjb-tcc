package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record ChainOfCustodyLedgerResponse(
        String loteReferencia,
        String digestColecaoSha256,
        String chaveCustodia,
        String sealedBy,
        Instant firstSealedAt,
        long totalEntradas,
        boolean integridadeOk,
        List<LedgerEntry> entradas
) {
    public boolean integrityOk() { return integridadeOk(); }

    public record LedgerEntry(
            int ordemLote,
            String evidenceId,
            String evidenceNome,
            String digestSha256,
            String evidenceChaveCustodia,
            String metadataDigestSha256,
            List<String> metadadosCanonicos,
            String prevHash,
            String entryHash,
            Instant sealedAt,
            String requestId
    ) {
    }
}
