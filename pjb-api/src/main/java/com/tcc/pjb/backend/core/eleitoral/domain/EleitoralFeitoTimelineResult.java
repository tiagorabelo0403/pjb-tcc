package com.tcc.pjb.backend.core.eleitoral.domain;

import java.util.List;

public record EleitoralFeitoTimelineResult(
        Long processoId,
        List<EleitoralFeitoTimelineEntry> entries
) {}
