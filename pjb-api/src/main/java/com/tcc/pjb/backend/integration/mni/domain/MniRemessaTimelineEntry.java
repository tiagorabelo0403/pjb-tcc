package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRemessaTimelineEntry(
        String evento,
        Instant instante,
        String detalhe
) {}
