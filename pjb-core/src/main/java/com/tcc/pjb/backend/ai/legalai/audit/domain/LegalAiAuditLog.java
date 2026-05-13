package com.tcc.pjb.backend.ai.legalai.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record LegalAiAuditLog(
        UUID id,
        LegalAiAuditAction acao,
        String entidadeTipo,
        String entidadeId,
        String atorId,
        String modelo,
        Integer inputTokens,
        Integer outputTokens,
        String detalhes,
        Instant criadoEm
) {

    public static LegalAiAuditLog iniciar(
            LegalAiAuditAction acao,
            String entidadeTipo,
            String entidadeId,
            String atorId,
            Instant agora) {
        return new LegalAiAuditLog(
                UUID.randomUUID(), acao, entidadeTipo, entidadeId,
                atorId, null, null, null, null, agora);
    }

    public LegalAiAuditLog comModelo(String modelo) {
        return new LegalAiAuditLog(id, acao, entidadeTipo, entidadeId, atorId,
                modelo, inputTokens, outputTokens, detalhes, criadoEm);
    }

    public LegalAiAuditLog comTokens(Integer inputTokens, Integer outputTokens) {
        return new LegalAiAuditLog(id, acao, entidadeTipo, entidadeId, atorId,
                modelo, inputTokens, outputTokens, detalhes, criadoEm);
    }

    public LegalAiAuditLog comDetalhes(String detalhes) {
        return new LegalAiAuditLog(id, acao, entidadeTipo, entidadeId, atorId,
                modelo, inputTokens, outputTokens, detalhes, criadoEm);
    }
}
