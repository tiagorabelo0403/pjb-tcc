package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.Instant;

public record EleitoralFeitoTimelineEntry(
        String evento,
        Instant instante,
        String detalhe
) {}
