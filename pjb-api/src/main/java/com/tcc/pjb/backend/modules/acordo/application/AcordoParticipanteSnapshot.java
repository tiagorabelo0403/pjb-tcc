package com.tcc.pjb.backend.modules.acordo.application;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoPapelParticipante;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoParticipanteStatus;
import java.time.Instant;

public record AcordoParticipanteSnapshot(
        Long id,
        Long sessaoId,
        Long usuarioId,
        AcordoPapelParticipante papel,
        AcordoParticipanteStatus status,
        Instant aceitouEm,
        Instant recusouEm,
        Instant createdAt
) {
    public boolean aceito() {
        return status == AcordoParticipanteStatus.ACEITO;
    }

    public AcordoParticipanteSnapshot withAceite(Instant now) {
        return new AcordoParticipanteSnapshot(id, sessaoId, usuarioId, papel, AcordoParticipanteStatus.ACEITO, now, recusouEm, createdAt);
    }

    public AcordoParticipanteSnapshot withRecusa(Instant now) {
        return new AcordoParticipanteSnapshot(id, sessaoId, usuarioId, papel, AcordoParticipanteStatus.RECUSADO, aceitouEm, now, createdAt);
    }
}
