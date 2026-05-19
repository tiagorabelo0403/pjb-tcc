package com.tcc.pjb.backend.modules.acordo.application;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoTermoStatus;
import java.time.Instant;

public record AcordoTermoSnapshot(
        Long id,
        Long sessaoId,
        Long propostaId,
        String conteudoTermo,
        String hashTermo,
        AcordoTermoStatus status,
        Instant createdAt
) {
    public AcordoTermoSnapshot withStatus(AcordoTermoStatus novoStatus) {
        return new AcordoTermoSnapshot(id, sessaoId, propostaId, conteudoTermo, hashTermo, novoStatus, createdAt);
    }
}
