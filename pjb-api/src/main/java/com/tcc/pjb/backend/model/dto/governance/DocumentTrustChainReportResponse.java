package com.tcc.pjb.backend.model.dto.governance;

import java.time.Instant;
import java.util.List;

public record DocumentTrustChainReportResponse(
        Long processoId,
        String numeroProcesso,
        int totalDocumentos,
        int totalEntradasCustodia,
        List<DocumentTrustChainDocumentView> documentos
) {
    public record DocumentTrustChainDocumentView(
            String documentoId,
            String titulo,
            String sha256,
            boolean custodioAtivo,
            String chaveCustodia,
            Instant ultimoSelo,
            String entryHash,
            String loteReferencia,
            String sealedByPerfil
    ) {
    }
}
