package com.tcc.pjb.backend.integration.mni.domain;

import java.util.List;

public record MniRemessaTimelineResult(
        Long remessaId,
        List<MniRemessaTimelineEntry> entries
) {}
