package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record ChainOfCustodySyncBundleResponse(
        String chaveCustodia,
        String digestColecaoSha256,
        String parceiroInstitucional,
        String noOrigem,
        String nonce,
        String justificativa,
        String payloadDigestSha256,
        String assinaturaHmacSha256,
        long totalEntradas,
        Instant exportadoEm,
        List<SyncLedgerEntry> entradas
) {

    public record SyncLedgerEntry(
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
