package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.Instant;

public record EleitoralDiplomacaoSyncSummary(
        boolean dryRun,
        int pendentesAntes,
        Instant executedAt
) {
}
