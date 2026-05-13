package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.time.Instant;
import java.util.UUID;

public record MemoryCandidateReview(
        MemoryCandidateReviewId id,
        UUID candidateId,
        String sessionId,
        String moduloOrigem,
        MemoryCandidateType tipo,
        String chave,
        String conteudo,
        String motivoExtracao,
        MemorySigiloNivel sigiloNivel,
        MemoryCandidateReviewStatus status,
        String revisadoPor,
        String motivoRejeicao,
        Instant criadoEm,
        Instant revisadoEm
) {

    public static MemoryCandidateReview criar(
            UUID candidateId,
            String sessionId,
            String moduloOrigem,
            MemoryCandidateType tipo,
            String chave,
            String conteudo,
            String motivoExtracao,
            MemorySigiloNivel sigiloNivel,
            Instant agora) {
        return new MemoryCandidateReview(
                MemoryCandidateReviewId.gerar(), candidateId, sessionId, moduloOrigem,
                tipo, chave, conteudo, motivoExtracao, sigiloNivel,
                MemoryCandidateReviewStatus.PENDENTE, null, null, agora, null);
    }

    public MemoryCandidateReview aprovado(String revisadoPor, Instant agora) {
        return new MemoryCandidateReview(
                id, candidateId, sessionId, moduloOrigem, tipo, chave, conteudo,
                motivoExtracao, sigiloNivel, MemoryCandidateReviewStatus.APROVADO,
                revisadoPor, null, criadoEm, agora);
    }

    public MemoryCandidateReview rejeitado(String revisadoPor, String motivo, Instant agora) {
        return new MemoryCandidateReview(
                id, candidateId, sessionId, moduloOrigem, tipo, chave, conteudo,
                motivoExtracao, sigiloNivel, MemoryCandidateReviewStatus.REJEITADO,
                revisadoPor, motivo, criadoEm, agora);
    }

    public boolean isPendente() {
        return status == MemoryCandidateReviewStatus.PENDENTE;
    }
}
