package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.time.Instant;
import java.util.UUID;

public record MemoryCandidate(
        UUID id,
        String sessionId,
        MemoryCandidateType tipo,
        String conteudo,
        String motivoExtracao,
        MemorySigiloNivel sigiloNivel,
        boolean aprovado,
        Instant criadoEm,
        Instant revisadoEm
) {

    public static MemoryCandidate extrair(
            String sessionId,
            MemoryCandidateType tipo,
            String conteudo,
            String motivo,
            MemorySigiloNivel nivel,
            Instant agora) {
        return new MemoryCandidate(UUID.randomUUID(), sessionId, tipo, conteudo, motivo, nivel, false, agora, null);
    }

    public MemoryCandidate aprovado(Instant agora) {
        return new MemoryCandidate(id, sessionId, tipo, conteudo, motivoExtracao, sigiloNivel, true, criadoEm, agora);
    }

    public boolean podePromoverAutomaticamente() {
        return !MemoryAccessPolicy.requerRevisaoHumana(sigiloNivel)
                && tipo != MemoryCandidateType.PROCESSUAL
                && tipo != MemoryCandidateType.JURIDICA_VALIDADA;
    }
}
