package com.tcc.pjb.backend.model.dto.acordo;

import java.time.Instant;

public record ChatAcordoSalaResponse(
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
}
