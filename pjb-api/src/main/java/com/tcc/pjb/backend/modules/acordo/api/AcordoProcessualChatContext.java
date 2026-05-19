package com.tcc.pjb.backend.modules.acordo.api;

import java.time.Instant;

public record AcordoProcessualChatContext(
        Long processoId,
        Long sessaoId,
        String status,
        String tipoSala,
        String confidencialidadeNivel,
        boolean segredoJustica,
        boolean salaAtiva,
        Instant expiraEm,
        int participantesAceitos
) {
    public static AcordoProcessualChatContext semSala(Long processoId) {
        return new AcordoProcessualChatContext(processoId, null, "SEM_SALA_ATIVA", null, null, false, false, null, 0);
    }
}
