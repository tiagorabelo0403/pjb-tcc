package com.tcc.pjb.backend.service.oficial_justica;

import java.time.Instant;

record OficialJusticaAgendaLiveEventDigest(
        int attempts,
        Instant latestAttemptAt,
        String frustrationCode,
        String frustrationLabel,
        String returnStrategy,
        boolean requiresReorder
) {
}
