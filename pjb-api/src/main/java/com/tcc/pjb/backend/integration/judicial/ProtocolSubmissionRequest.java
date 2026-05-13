package com.tcc.pjb.backend.integration.judicial;

import java.util.Map;

public record ProtocolSubmissionRequest(
        String requestId,
        String numeroUnificado,
        String title,
        String tribunalCodigo,
        String unidadeJudiciariaCodigo,
        String unidadeJudiciariaNome,
        String rito,
        String classeTpu,
        String ramoDireito,
        String payloadJson,
        String integrityHash,
        Long signerUserId,
        Long executorUserId,
        boolean dryRun,
        Map<String, Object> metadata
) {
    public ProtocolSubmissionRequest {
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }
}
