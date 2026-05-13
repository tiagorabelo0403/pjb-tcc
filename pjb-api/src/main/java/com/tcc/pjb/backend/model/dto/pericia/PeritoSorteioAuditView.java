package com.tcc.pjb.backend.model.dto.pericia;

import java.time.Instant;
import java.util.List;

public record PeritoSorteioAuditView(
        Long id,
        Long processoId,
        Long actorId,
        String actorNome,
        Long peritoId,
        String peritoNome,
        String especialidadeCodigo,
        String comarca,
        String dataReferencia,
        double score,
        long nomeacoesAtivas,
        int candidatosElegiveis,
        List<String> fundamentos,
        String hashIntegridade,
        Instant createdAt
) {
}
