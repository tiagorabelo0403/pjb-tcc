package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ChainOfCustodySyncReplayRequest(
        @NotBlank String parceiroInstitucional,
        @NotBlank String noOrigem,
        @NotBlank String nonce,
        @NotBlank String chaveCustodia,
        @NotBlank String digestColecaoSha256,
        @NotBlank String payloadDigestSha256,
        @NotBlank String assinaturaHmacSha256,
        @NotEmpty List<@Valid SyncLedgerEntry> entradas
) {

    public record SyncLedgerEntry(
            int ordemLote,
            @NotBlank String evidenceId,
            @NotBlank String evidenceNome,
            @NotBlank String digestSha256,
            @NotBlank String evidenceChaveCustodia,
            @NotBlank String metadataDigestSha256,
            List<String> metadadosCanonicos,
            String prevHash,
            @NotBlank String entryHash,
            Instant sealedAt,
            String requestId
    ) {
    }
}
