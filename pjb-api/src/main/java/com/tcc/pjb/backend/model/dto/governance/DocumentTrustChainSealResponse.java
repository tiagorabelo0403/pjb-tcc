package com.tcc.pjb.backend.model.dto.governance;

import java.time.Instant;

public record DocumentTrustChainSealResponse(
        Long processoId,
        String numeroProcesso,
        String documentoId,
        String documentoTitulo,
        String digestDocumento,
        String digestColecao,
        String chaveCustodia,
        String entryHash,
        boolean persistido,
        boolean duplicado,
        Instant sealedAt,
        String sealedByPerfil,
        String loteReferencia
) {
}
